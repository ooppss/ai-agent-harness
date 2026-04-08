package com.yourorg.usbdaemon.storage;

import com.yourorg.usbdaemon.config.AppConfig;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MinioStorageClient {
    interface MinioGateway {
        boolean bucketExists(String bucket) throws Exception;

        boolean objectExists(String bucket, String objectKey) throws Exception;

        WriteResult upload(Path sourceFile, String bucket, String objectKey) throws Exception;
    }

    record WriteResult(String etag, String versionId) {
    }

    private final AppConfig config;
    private final MinioGateway minioGateway;

    public MinioStorageClient(AppConfig config) {
        this(
                config,
                MinioClient.builder()
                .endpoint(config.getMinioEndpoint())
                .credentials(config.getMinioAccessKey(), config.getMinioSecretKey())
                .build());
    }

    MinioStorageClient(AppConfig config, MinioClient minioClient) {
        this(config, new SdkMinioGateway(Objects.requireNonNull(minioClient, "minioClient")));
    }

    MinioStorageClient(AppConfig config, MinioGateway minioGateway) {
        this.config = Objects.requireNonNull(config, "config");
        this.minioGateway = Objects.requireNonNull(minioGateway, "minioGateway");
    }

    public StorageResponse upload(Path sourceFile, String bucket, String objectKey) {
        if (!Files.isRegularFile(sourceFile)) {
            return new StorageResponse(bucket, objectKey, StorageStatus.FAILURE, "Source file does not exist or is not a regular file", null, null);
        }
        if (bucket == null || bucket.isBlank()) {
            return new StorageResponse(bucket, objectKey, StorageStatus.FAILURE, "Bucket name must not be blank", null, null);
        }
        if (objectKey == null || objectKey.isBlank()) {
            return new StorageResponse(bucket, objectKey, StorageStatus.FAILURE, "Object key must not be blank", null, null);
        }

        try {
            if (!bucketExists(bucket)) {
                return new StorageResponse(bucket, objectKey, StorageStatus.FAILURE, "Bucket does not exist", null, null);
            }

            if (objectExists(bucket, objectKey)) {
                return new StorageResponse(
                        bucket,
                        objectKey,
                        StorageStatus.SKIPPED,
                        "Object already exists. Upload skipped",
                        null,
                        null);
            }

            WriteResult response = minioGateway.upload(sourceFile, bucket, objectKey);

            return new StorageResponse(
                    bucket,
                    objectKey,
                    StorageStatus.SUCCESS,
                    "Upload completed",
                    response.etag(),
                    response.versionId());
        } catch (Exception exception) {
            return new StorageResponse(
                    bucket,
                    objectKey,
                    StorageStatus.FAILURE,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    null,
                    null);
        }
    }

    public boolean bucketExists(String bucket) throws Exception {
        return minioGateway.bucketExists(bucket);
    }

    public boolean objectExists(String bucket, String objectKey) throws Exception {
        return minioGateway.objectExists(bucket, objectKey);
    }

    public enum StorageStatus {
        SUCCESS,
        SKIPPED,
        FAILURE
    }

    public record StorageResponse(
            String bucket,
            String objectKey,
            StorageStatus status,
            String detailMessage,
            String etag,
            String versionId) {
        public boolean isSuccess() {
            return status == StorageStatus.SUCCESS;
        }

        public boolean isSkipped() {
            return status == StorageStatus.SKIPPED;
        }

        public boolean isFailure() {
            return status == StorageStatus.FAILURE;
        }
    }

    private static final class SdkMinioGateway implements MinioGateway {
        private final MinioClient minioClient;

        private SdkMinioGateway(MinioClient minioClient) {
            this.minioClient = minioClient;
        }

        @Override
        public boolean bucketExists(String bucket) throws Exception {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        }

        @Override
        public boolean objectExists(String bucket, String objectKey) throws Exception {
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .build());
                return true;
            } catch (ErrorResponseException exception) {
                String errorCode = exception.errorResponse().code();
                if ("NoSuchKey".equals(errorCode) || "NoSuchObject".equals(errorCode)) {
                    return false;
                }
                throw exception;
            }
        }

        @Override
        public WriteResult upload(Path sourceFile, String bucket, String objectKey) throws Exception {
            ObjectWriteResponse response = minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .filename(sourceFile.toString())
                            .contentType("application/octet-stream")
                            .build());
            return new WriteResult(response.etag(), response.versionId());
        }
    }
}
