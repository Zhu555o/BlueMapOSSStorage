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

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import java.util.Objects;

final class OSSClientFactory {

    private OSSClientFactory() {}

    public static OSS build(OSSConfiguration cfg) {
        Objects.requireNonNull(cfg, "cfg");
        if (cfg.getEndpoint() == null || cfg.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (cfg.getAccessKeyId() == null || cfg.getAccessKeyId().isBlank()) {
            throw new IllegalArgumentException("accessKeyId is required");
        }
        if (cfg.getAccessKeySecret() == null || cfg.getAccessKeySecret().isBlank()) {
            throw new IllegalArgumentException("accessKeySecret is required");
        }
        if (cfg.getBucketName() == null || cfg.getBucketName().isBlank()) {
            throw new IllegalArgumentException("bucketName is required");
        }

        try {
            String endpoint = cfg.getEndpoint();
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                endpoint = "https://" + endpoint;
            }
            
            return new OSSClientBuilder().build(endpoint, cfg.getAccessKeyId(), cfg.getAccessKeySecret());
        } catch (ClientException | OSSException e) {
            throw new IllegalStateException("Failed to build OSS Client", e);
        }
    }
}
