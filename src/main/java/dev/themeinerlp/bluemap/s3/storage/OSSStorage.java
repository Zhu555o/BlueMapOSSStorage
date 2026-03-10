/**
 * A simple storage implementation for bluemap to save data into s3 storage solution.
 * Copyright (C) 2025 TheMeinerLP and contributors
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.themeinerlp.bluemap.s3.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ObjectListing;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoublePredicate;
import java.util.stream.Stream;

public final class OSSStorage implements Storage {

    private final OSSConfiguration configuration;
    private final Compression compression;
    private OSS ossClient;
    private boolean closed = false;
    private final LoadingCache<String, OSSMapStorage> mapStorages;

    public OSSStorage(OSSConfiguration configuration, Compression compression) {
        this.configuration = configuration;
        this.compression = compression;
        mapStorages = Caffeine.newBuilder().build(this::create);
    }

    @Override
    public void initialize() throws IOException {
        if (isClosed()) {
            throw new IOException("Storage is closed");
        }

        try {
            this.ossClient = OSSClientFactory.build(configuration);
            String bucketName = configuration.getBucketName();
            if (!ossClient.doesBucketExist(bucketName)) {
                throw new IOException("Bucket '" + bucketName + "' does not exist");
            }
        } catch (Exception e) {
            throw new IOException("Failed to initialize OSS storage", e);
        }
    }

    public OSSMapStorage create(String mapId) {
        return new OSSMapStorage(ossClient, configuration, mapId, compression);
    }

    @Override
    public MapStorage map(String mapId) {
        if (isClosed()) {
            throw new IllegalStateException("Storage is closed");
        }

        if (mapId == null || mapId.isEmpty()) {
            throw new IllegalArgumentException("Map ID cannot be null or empty");
        }
        return mapStorages.get(mapId);
    }

    @Override
    public Stream<String> mapIds() throws IOException {
        if (isClosed()) {
            throw new IOException("Storage is closed");
        }

        try {
            String rootPath = configuration.getRootPath();
            List<String> mapIds = new ArrayList<>();

            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(configuration.getBucketName());
            listObjectsRequest.setPrefix(rootPath + "/");
            listObjectsRequest.setDelimiter("/");

            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);

            for (String commonPrefix : objectListing.getCommonPrefixes()) {
                String mapId = commonPrefix
                        .substring(rootPath.length() + 1)
                        .replace("/", "");
                if (!mapId.isEmpty()) {
                    mapIds.add(mapId);
                }
            }

            return mapIds.stream();
        } catch (Exception e) {
            throw new IOException("Failed to list map IDs", e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            if (ossClient != null) {
                ossClient.shutdown();
            }
            closed = true;
        }
    }

    public static final class OSSMapStorage implements MapStorage {
        private final OSS ossClient;
        private final OSSConfiguration configuration;
        private final String mapId;
        private final Compression compression;
        private final Path tempRoot;

        private final GridStorage hiresGridStorage;
        private final LoadingCache<Integer, GridStorage> lowresGridStorages;
        private final GridStorage tileStateStorage;
        private final GridStorage chunkStateStorage;

        public OSSMapStorage(OSS ossClient, OSSConfiguration configuration, String mapId, Compression compression) {
            this.ossClient = ossClient;
            this.configuration = configuration;
            this.mapId = mapId;
            this.compression = compression;

            try {
                this.tempRoot = Files.createTempDirectory("oss-storage-" + mapId);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }

            this.hiresGridStorage = new OSSGridStorage(
                    ossClient,
                    configuration,
                    mapId,
                    "tiles/0",
                    ".prbm" + compression.getFileSuffix(),
                    compression);

            this.lowresGridStorages = Caffeine.newBuilder().build(lod -> new OSSGridStorage(
                    ossClient,
                    configuration,
                    mapId,
                    "tiles/" + lod,
                    ".png",
                    Compression.NONE));

            this.tileStateStorage = new OSSGridStorage(
                    ossClient,
                    configuration,
                    mapId,
                    "rstate",
                    ".tiles.dat",
                    Compression.GZIP);

            this.chunkStateStorage = new OSSGridStorage(
                    ossClient,
                    configuration,
                    mapId,
                    "rstate",
                    ".chunks.dat",
                    Compression.GZIP);
        }

        @Override
        public GridStorage hiresTiles() {
            return hiresGridStorage;
        }

        @Override
        public GridStorage lowresTiles(int lod) {
            return lowresGridStorages.get(lod);
        }

        @Override
        public GridStorage tileState() {
            return tileStateStorage;
        }

        @Override
        public GridStorage chunkState() {
            return chunkStateStorage;
        }

        @Override
        public ItemStorage asset(String name) {
            String[] parts = name.split("/");
            StringBuilder keyBuilder = new StringBuilder();

            String rootPath = configuration.getRootPath();
            if (!rootPath.equals(".") && !rootPath.isEmpty()) {
                keyBuilder.append(rootPath).append("/");
            }
            keyBuilder.append(mapId).append("/assets/");

            for (String part : parts) {
                keyBuilder.append(part).append("/");
            }

            String key = keyBuilder.toString();
            return new OSSItemStorage(ossClient, configuration.getBucketName(), key, Compression.NONE);
        }

        @Override
        public ItemStorage settings() {
            String key = buildKey("settings.json");
            return new OSSItemStorage(ossClient, configuration.getBucketName(), key, Compression.NONE);
        }

        @Override
        public ItemStorage textures() {
            String key = buildKey("textures.json" + compression.getFileSuffix());
            return new OSSItemStorage(ossClient, configuration.getBucketName(), key, compression);
        }

        @Override
        public ItemStorage markers() {
            String key = buildKey("live/markers.json");
            return new OSSItemStorage(ossClient, configuration.getBucketName(), key, Compression.NONE);
        }

        @Override
        public ItemStorage players() {
            String key = buildKey("live/players.json");
            return new OSSItemStorage(ossClient, configuration.getBucketName(), key, Compression.NONE);
        }

        @Override
        public void delete(DoublePredicate onProgress) throws IOException {
            try {
                String rootPath = configuration.getRootPath();
                String prefix = rootPath.equals(".") || rootPath.isEmpty() ? mapId + "/" : rootPath + "/" + mapId + "/";

                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(configuration.getBucketName());
                listObjectsRequest.setPrefix(prefix);

                ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
                List<String> keys = new ArrayList<>();

                for (com.aliyun.oss.model.OSSObjectSummary summary : objectListing.getObjectSummaries()) {
                    keys.add(summary.getKey());
                }

                int total = keys.size();
                int deleted = 0;

                for (String key : keys) {
                    ossClient.deleteObject(configuration.getBucketName(), key);
                    deleted++;

                    if (!onProgress.test(deleted / (double) total)) {
                        return;
                    }
                }
            } catch (Exception e) {
                throw new IOException("Failed to delete map", e);
            }
        }

        @Override
        public boolean exists() throws IOException {
            try {
                String rootPath = configuration.getRootPath();
                String prefix = rootPath.equals(".") || rootPath.isEmpty() ? mapId + "/" : rootPath + "/" + mapId + "/";

                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(configuration.getBucketName());
                listObjectsRequest.setPrefix(prefix);
                listObjectsRequest.setMaxKeys(1);

                ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
                return !objectListing.getObjectSummaries().isEmpty();
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean isClosed() {
            return ossClient == null;
        }

        private String buildKey(String filename) {
            String rootPath = configuration.getRootPath();
            if (rootPath.equals(".") || rootPath.isEmpty()) {
                return mapId + "/" + filename;
            } else {
                return rootPath + "/" + mapId + "/" + filename;
            }
        }
    }
}
