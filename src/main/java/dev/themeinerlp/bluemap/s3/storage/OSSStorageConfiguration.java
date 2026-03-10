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

import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.debug.DebugDump;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.Key;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public final class OSSStorageConfiguration extends StorageConfig implements OSSConfiguration {

    @Comment("The OSS endpoint URL (e.g., oss-cn-hangzhou.aliyuncs.com for Aliyun OSS)")
    private String endpoint = "oss-cn-hangzhou.aliyuncs.com";

    @Comment("The Access Key ID for OSS authentication")
    private String accessKeyId = "your-access-key-id";

    @Comment("The Access Key Secret for OSS authentication")
    @DebugDump(exclude = true)
    private String accessKeySecret = "your-access-key-secret";

    @Comment("The name of the OSS bucket to use")
    private String bucketName = "bluemap-storage";

    @Comment("The compression type to use for storing data (default: gzip)")
    private String compression = "gzip";

    @Comment("The root path in the OSS bucket (default: empty, meaning the root of the bucket)")
    private String rootPath = ".";

    @Override
    public Storage createStorage() throws ConfigurationException {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new ConfigurationException("OSS endpoint is required");
        }

        if (accessKeyId == null || accessKeyId.isEmpty()) {
            throw new ConfigurationException("OSS Access Key ID is required");
        }

        if (accessKeySecret == null || accessKeySecret.isEmpty()) {
            throw new ConfigurationException("OSS Access Key Secret is required");
        }

        if (bucketName == null || bucketName.isEmpty()) {
            throw new ConfigurationException("OSS bucket name is required");
        }

        return new OSSStorage(this, getCompression());
    }

    public Compression getCompression() {
        return Compression.REGISTRY.get(Key.bluemap(compression));
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String getRootPath() {
        return rootPath != null ? rootPath.trim() : ".";
    }
}
