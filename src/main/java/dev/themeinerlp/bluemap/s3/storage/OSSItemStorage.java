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
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OSSItemStorage implements ItemStorage {

    private final OSS ossClient;
    private final String bucketName;
    private final String key;
    private final Compression compression;

    public OSSItemStorage(OSS ossClient, String bucketName, String key, Compression compression) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.key = key;
        this.compression = compression;
    }

    @Override
    public OutputStream write() throws IOException {
        return new OSSOutputStream(ossClient, bucketName, key, compression);
    }

    @Override
    public CompressedInputStream read() throws IOException {
        try {
            if (!ossClient.doesObjectExist(bucketName, key)) {
                return null;
            }

            OSSObject ossObject = ossClient.getObject(bucketName, key);
            return new CompressedInputStream(ossObject.getObjectContent(), compression);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void delete() throws IOException {
        try {
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            throw new IOException("Failed to delete object from OSS", e);
        }
    }

    @Override
    public boolean exists() throws IOException {
        try {
            return ossClient.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            throw new IOException("Failed to check if object exists", e);
        }
    }

    @Override
    public boolean isClosed() {
        return ossClient == null;
    }

    private static class OSSOutputStream extends OutputStream {
        private final OSS ossClient;
        private final String bucketName;
        private final String key;
        private final Compression compression;
        private final ByteArrayOutputStream buffer;

        public OSSOutputStream(OSS ossClient, String bucketName, String key, Compression compression) {
            this.ossClient = ossClient;
            this.bucketName = bucketName;
            this.key = key;
            this.compression = compression;
            this.buffer = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            buffer.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            super.close();
            try {
                byte[] data = buffer.toByteArray();
                ByteArrayOutputStream compressedBuffer = new ByteArrayOutputStream();
                try (OutputStream compressedStream = compression.compress(compressedBuffer)) {
                    compressedStream.write(data);
                }
                byte[] compressedData = compressedBuffer.toByteArray();

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(compressedData.length);

                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        bucketName,
                        key,
                        new ByteArrayInputStream(compressedData),
                        metadata);

                ossClient.putObject(putObjectRequest);
            } catch (Exception e) {
                throw new IOException("Failed to write to OSS", e);
            }
        }
    }
}
