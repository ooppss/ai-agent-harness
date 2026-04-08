package com.yourorg.usbdaemon.storage;

import com.yourorg.usbdaemon.config.AppConfig;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadObjectArgs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MinioStorageClient {
    private final AppConfig config;
    private final MinioClient minioClient;

    public MinioStorageClient(AppConfig config) {
        this(config, MinioClient.builder()
                .endpoint(config.getMinioEndpoint())
                .credentials(config.getMinioAccessKey(), config.getMinioSecretKey())
                .build());
    }

    MinioStorageClient(AppConfig config, MinioClient minioClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient");
    }

    public StorageResponse upload(Path sourceFile, String bucket, String objectKey) {
        if (!Files.isRegularFile(sourceFile)) {
            return new StorageResponse(bucket, objectKey, false, "Source file does not exist or is not a regular file", null, null);
        }
        if (bucket == null || bucket.isBlank()) {
            return new StorageResponse(bucket, objectKey, false, "Bucket name must not be blank", null, null);
        }
        if (objectKey == null || objectKey.isBlank()) {
            return new StorageResponse(bucket, objectKey, false, "Object key must not be blank", null, null);
        }

        try {
            if (!bucketExists(bucket)) {
                return new StorageResponse(bucket, objectKey, false, "Bucket does not exist", null, null);
            }

            ObjectWriteResponse response = minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .filename(sourceFile.toString())
                            .contentType("application/octet-stream")
                            .build());

            return new StorageResponse(
                    bucket,
                    objectKey,
                    true,
                    "Upload completed",
                    response.etag(),
                    response.versionId());
        } catch (Exception exception) {
            return new StorageResponse(
                    bucket,
                    objectKey,
                    false,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    null,
                    null);
        }
    }

    public boolean bucketExists(String bucket) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
    }

    public record StorageResponse(
            String bucket,
            String objectKey,
            boolean success,
            String detailMessage,
            String etag,
            String versionId) {
    }
}
