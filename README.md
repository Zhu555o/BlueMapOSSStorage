Implemented by Qwen3.5 Plus in Trae CN

# BlueMapOSSStorage

A storage addon for [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) that enables storing map data in Aliyun OSS (Object Storage Service).

## Overview

BlueMapOSSStorage is an addon for BlueMap that provides the ability to store map data in Aliyun OSS (Object Storage Service). This addon is particularly useful for:

- Servers deployed in China or Asia regions
- Environments where local storage is limited or not persistent
- Setups that require high availability and redundancy
- Multi-server networks that need to share the same map data
- Cost-effective object storage solutions

## Features

- Store BlueMap data directly in Aliyun OSS
- Support for all OSS regions worldwide
- Automatic compression with gzip, zstd, or deflate
- Seamless integration with BlueMap's storage system
- High performance with built-in caching
- Cost-effective storage solution

## Requirements

- BlueMap 5.3 or higher
- Java 21 or higher
- Aliyun OSS bucket and credentials

## Installation

### Spigot/Paper

1. Download the latest release of BlueMapOSSStorage from the [releases page](https://github.com/Zhu555o/BlueMapOSSStorage/releases)
2. Place the JAR file in the `./plugins/BlueMap/packs/` directory of your server
3. Restart your server or reload BlueMap

### Sponge, Forge, Fabric

1. Download the latest release of BlueMapOSSStorage from the [releases page](https://github.com/Zhu555o/BlueMapOSSStorage/releases)
2. Place the JAR file in the `./config/bluemap/packs/` directory of your server
3. Restart your server or reload BlueMap

### CLI

1. Download the latest release of BlueMapOSSStorage from the [releases page](https://github.com/Zhu555o/BlueMapOSSStorage/releases)
2. Place the JAR file in the `./config/packs/` directory of your server
3. Restart your server or reload BlueMap

## Configuration

To use Aliyun OSS storage with BlueMap, you need to create or modify the OSS storage configuration file. Create a file named `oss.conf` in the `plugins/BlueMap/storages` directory with the following content:

```hocon
##                          ##
##         BlueMap          ##
##      Storage-Config      ##
##                          ##

# The storage-type of this storage.
# Depending on this setting, different config-entries are allowed/expected in this config file.
# Don't change this value! (If you want a different storage-type, check out the other example-configs)
storage-type: "themeinerlp:oss"

# The compression-type that bluemap will use to compress generated map-data.
# Available compression-types are:
#  - gzip
#  - zstd
#  - deflate
#  - none
# The default is: gzip
compression: gzip

# The OSS endpoint URL (e.g., oss-cn-hangzhou.aliyuncs.com for Aliyun OSS)
endpoint: "oss-cn-hangzhou.aliyuncs.com"

# OSS credentials
access-key-id: "your-access-key-id"
access-key-secret: "your-access-key-secret"

# The name of the OSS bucket to use
bucket-name: "bluemap-storage"

# Optional: The root path in the OSS bucket where BlueMap data will be stored
# Default is "." (root of the bucket)
root-path: "."
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `storage-type` | The storage type identifier (don't change this value) | `themeinerlp:oss` |
| `compression` | The compression type to use for storing data (options: "gzip", "zstd", "deflate", "none") | `gzip` |
| `endpoint` | The OSS endpoint URL for your region | `oss-cn-hangzhou.aliyuncs.com` |
| `access-key-id` | Your Aliyun Access Key ID | Required |
| `access-key-secret` | Your Aliyun Access Key Secret | Required |
| `bucket-name` | The name of the OSS bucket to use | `bluemap-storage` |
| `root-path` | Optional: The root path in the OSS bucket where BlueMap data will be stored | `.` |

### Common OSS Endpoints

| Region | Endpoint |
|--------|----------|
| East China 1 (Hangzhou) | `oss-cn-hangzhou.aliyuncs.com` |
| East China 2 (Shanghai) | `oss-cn-shanghai.aliyuncs.com` |
| North China 1 (Qingdao) | `oss-cn-qingdao.aliyuncs.com` |
| North China 2 (Beijing) | `oss-cn-beijing.aliyuncs.com` |
| South China 1 (Shenzhen) | `oss-cn-shenzhen.aliyuncs.com` |
| Hong Kong | `oss-cn-hongkong.aliyuncs.com` |
| US West 1 (Silicon Valley) | `oss-us-west-1.aliyuncs.com` |
| Singapore | `oss-ap-southeast-1.aliyuncs.com` |

For a complete list of OSS regions and endpoints, see the [Aliyun OSS documentation](https://www.alibabacloud.com/help/en/oss/user-guide/regions-and-endpoints).

## Usage Examples

### Hangzhou Region

```hocon
##                          ##
##         BlueMap          ##
##      Storage-Config      ##
##                          ##

storage-type: "themeinerlp:oss"
compression: gzip

endpoint: "oss-cn-hangzhou.aliyuncs.com"
access-key-id: "LTAI5t...your-key-id"
access-key-secret: "your-secret-key"
bucket-name: "my-bluemap-storage"
root-path: "."
```

### Shanghai Region

```hocon
##                          ##
##         BlueMap          ##
##      Storage-Config      ##
##                          ##

storage-type: "themeinerlp:oss"
compression: gzip

endpoint: "oss-cn-shanghai.aliyuncs.com"
access-key-id: "LTAI5t...your-key-id"
access-key-secret: "your-secret-key"
bucket-name: "bluemap-maps"
root-path: "server1"
```

### Custom Path with Compression

```hocon
##                          ##
##         BlueMap          ##
##      Storage-Config      ##
##                          ##

storage-type: "themeinerlp:oss"
compression: zstd

endpoint: "oss-cn-beijing.aliyuncs.com"
access-key-id: "your-access-key-id"
access-key-secret: "your-access-key-secret"
bucket-name: "game-storage"
root-path: "bluemap/survival"
```

## Getting Aliyun OSS Credentials

1. Log in to the [Aliyun Console](https://homenew.console.aliyun.com/)
2. Navigate to **Object Storage Service (OSS)**
3. Create a new bucket or select an existing one
4. Go to **AccessKey Management** in your account settings
5. Create a new AccessKey or use an existing one
6. Copy the AccessKey ID and AccessKey Secret to your configuration file

**Security Note:** Keep your AccessKey Secret secure and never share it publicly. Consider using RAM (Resource Access Management) users with limited permissions for better security.

## Building from Source

1. Clone the repository:
   ```
   git clone https://github.com/Zhu555o/BlueMapOSSStorage.git
   ```

2. Build the project using Gradle:
   ```
   ./gradlew shadowJar
   ```

3. The built JAR file will be located in the `build/libs` directory.

## Migration from AWS S3

If you are migrating from the AWS S3 storage addon, you need to:

1. Update the `storage-type` from `themeinerlp:s3` to `themeinerlp:oss`
2. Change `region` to the appropriate OSS `endpoint`
3. Rename `secret-access-key` to `access-key-secret`
4. Remove `force-path-style` and `endpoint-url` options (not needed for OSS)
5. Update your credentials to Aliyun AccessKey

## License

This project is licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE).

## Credits

- Developed by [TheMeinerLP](https://github.com/TheMeinerLP) and [contributors](https://github.com/TheMeinerLP/BlueMapS3Storage/graphs/contributors)
- Migrated to Aliyun OSS by [Zhu555o](https://github.com/Zhu555o)
- Uses [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) by [Blue](https://github.com/TBlueF)
- Uses [Aliyun OSS SDK for Java](https://github.com/aliyun/aliyun-oss-java-sdk) for OSS integration

## Support

If you encounter any issues or have questions, please [open an issue](https://github.com/Zhu555o/BlueMapOSSStorage/issues) on the GitHub repository.
