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
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class OSSGridStorage implements GridStorage {

    private static final Pattern ITEM_PATH_PATTERN = Pattern.compile("x(-?\\d+)z(-?\\d+)");

    private final OSS ossClient;
    private final OSSConfiguration configuration;
    private final String mapId;
    private final String prefix;
    private final String suffix;
    private final Compression compression;

    public OSSGridStorage(OSS ossClient, OSSConfiguration configuration, String mapId, String prefix, String suffix, Compression compression) {
        this.ossClient = ossClient;
        this.configuration = configuration;
        this.mapId = mapId;
        this.prefix = prefix;
        this.suffix = suffix;
        this.compression = compression;
    }

    @Override
    public OutputStream write(int x, int z) throws IOException {
        return cell(x, z).write();
    }

    @Override
    public CompressedInputStream read(int x, int z) throws IOException {
        return cell(x, z).read();
    }

    @Override
    public void delete(int x, int z) throws IOException {
        cell(x, z).delete();
    }

    @Override
    public boolean exists(int x, int z) throws IOException {
        return cell(x, z).exists();
    }

    @Override
    public ItemStorage cell(int x, int z) {
        String key = buildKey(getItemPath(x, z));
        return new OSSItemStorage(ossClient, configuration.getBucketName(), key, compression);
    }

    @Override
    public Stream<Cell> stream() throws IOException {
        try {
            String rootPath = configuration.getRootPath();
            String searchPrefix = buildKey(prefix);
            
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(configuration.getBucketName());
            listObjectsRequest.setPrefix(searchPrefix);
            
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            
            return objectListing.getObjectSummaries().stream()
                    .<Cell>map(summary -> {
                        String key = summary.getKey();
                        if (!key.endsWith(suffix)) return null;
                        
                        String relativeKey = key.substring(searchPrefix.length());
                        relativeKey = relativeKey.substring(0, relativeKey.length() - suffix.length());
                        relativeKey = relativeKey.replace("/", "");
                        
                        Matcher matcher = ITEM_PATH_PATTERN.matcher(relativeKey);
                        if (!matcher.matches()) return null;
                        
                        int x = Integer.parseInt(matcher.group(1));
                        int z = Integer.parseInt(matcher.group(2));
                        
                        return new OSSCell(x, z, ossClient, configuration.getBucketName(), key, compression);
                    })
                    .filter(Objects::nonNull);
        } catch (Exception e) {
            throw new IOException("Failed to stream grid cells", e);
        }
    }

    @Override
    public boolean isClosed() {
        return ossClient == null;
    }

    public String getItemPath(int x, int z) {
        String encodedPosition = "x" + x + "z" + z;

        LinkedList<String> folders = new LinkedList<>();
        StringBuilder folder = new StringBuilder();
        for (int i = 0; i < encodedPosition.length(); i++) {
            char c = encodedPosition.charAt(i);
            folder.append(c);
            if (c >= '0' && c <= '9') {
                folders.add(folder.toString());
                folder.delete(0, folder.length());
            }
        }

        String fileName = folders.removeLast();
        folders.add(fileName + suffix);

        StringBuilder pathBuilder = new StringBuilder(prefix);
        for (String part : folders) {
            pathBuilder.append("/").append(part);
        }

        return pathBuilder.toString();
    }

    private String buildKey(String path) {
        String rootPath = configuration.getRootPath();
        if (rootPath.equals(".") || rootPath.isEmpty()) {
            return mapId + "/" + path;
        } else {
            return rootPath + "/" + mapId + "/" + path;
        }
    }

    private static class OSSCell extends OSSItemStorage implements Cell {

        private final int x, z;

        public OSSCell(int x, int z, OSS ossClient, String bucketName, String key, Compression compression) {
            super(ossClient, bucketName, key, compression);
            this.x = x;
            this.z = z;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getZ() {
            return z;
        }
    }
}
