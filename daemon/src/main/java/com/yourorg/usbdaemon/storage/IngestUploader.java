package com.yourorg.usbdaemon.storage;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.naming.ObjectKeyBuilder;
import com.yourorg.usbdaemon.scan.ScanResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class IngestUploader {
    @FunctionalInterface
    interface UploadExecutor {
        MinioStorageClient.StorageResponse upload(Path sourceFile, String bucket, String objectKey);
    }

    private final AppConfig config;
    private final ObjectKeyBuilder objectKeyBuilder;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;
    private final UploadExecutor uploadExecutor;

    public IngestUploader(
            AppConfig config,
            MinioStorageClient minioStorageClient,
            ObjectKeyBuilder objectKeyBuilder,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger) {
        this(config, minioStorageClient::upload, objectKeyBuilder, daemonLogger, errorLogger);
    }

    IngestUploader(
            AppConfig config,
            UploadExecutor uploadExecutor,
            ObjectKeyBuilder objectKeyBuilder,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger) {
        this.config = Objects.requireNonNull(config, "config");
        this.uploadExecutor = Objects.requireNonNull(uploadExecutor, "uploadExecutor");
        this.objectKeyBuilder = Objects.requireNonNull(objectKeyBuilder, "objectKeyBuilder");
        this.daemonLogger = Objects.requireNonNull(daemonLogger, "daemonLogger");
        this.errorLogger = Objects.requireNonNull(errorLogger, "errorLogger");
    }

    public UploadBatchResult upload(ScanResult scanResult) {
        if (!scanResult.isSuccessful()) {
            return new UploadBatchResult(
                    config.getIngestBucket(),
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    "Upload skipped because scan result was not successful");
        }

        List<UploadResult> results = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        for (Path pcapFile : scanResult.getPcapFiles()) {
            try {
                String objectKey = objectKeyBuilder.build(scanResult.getMountPath(), pcapFile);
                daemonLogger.logObjectKeyBuilt(pcapFile, objectKey);
                daemonLogger.logUploadStart(pcapFile, config.getIngestBucket(), objectKey);
                MinioStorageClient.StorageResponse response =
                        uploadExecutor.upload(pcapFile, config.getIngestBucket(), objectKey);
                if (response.isFailure()) {
                    errorLogger.logUploadFailure(pcapFile, response.bucket(), response.objectKey(), response.detailMessage());
                } else if (response.isSkipped()) {
                    skippedCount++;
                    daemonLogger.logUploadSkipped(pcapFile, response.bucket(), response.objectKey(), response.detailMessage());
                } else {
                    successCount++;
                }
                daemonLogger.logUploadFinish(
                        pcapFile,
                        response.bucket(),
                        response.objectKey(),
                        response.isSuccess(),
                        response.etag());
                results.add(new UploadResult(
                        pcapFile,
                        response.bucket(),
                        response.objectKey(),
                        response.status(),
                        response.detailMessage(),
                        response.etag(),
                        response.versionId()));
            } catch (RuntimeException exception) {
                errorLogger.logObjectKeyFailure(pcapFile, exception);
                results.add(new UploadResult(
                        pcapFile,
                        config.getIngestBucket(),
                        null,
                        MinioStorageClient.StorageStatus.FAILURE,
                        exception.getMessage(),
                        null,
                        null));
            }
        }

        int totalCount = results.size();
        int failureCount = totalCount - successCount - skippedCount;
        String detailMessage = failureCount == 0
                ? (skippedCount == 0
                        ? "All upload requests completed successfully"
                        : "Upload batch completed with skipped duplicate objects")
                : "One or more upload requests failed";
        UploadBatchResult uploadBatchResult = new UploadBatchResult(
                config.getIngestBucket(),
                totalCount,
                successCount,
                skippedCount,
                failureCount,
                List.copyOf(results),
                detailMessage);
        daemonLogger.logUploadBatchFinish(
                uploadBatchResult.bucket(),
                uploadBatchResult.totalCount(),
                uploadBatchResult.successCount(),
                uploadBatchResult.skippedCount(),
                uploadBatchResult.failureCount(),
                uploadBatchResult.detailMessage());
        return uploadBatchResult;
    }

    public record UploadBatchResult(
            String bucket,
            int totalCount,
            int successCount,
            int skippedCount,
            int failureCount,
            List<UploadResult> results,
            String detailMessage) {
        public boolean isSuccessful() {
            return failureCount == 0;
        }
    }

    public record UploadResult(
            Path sourceFile,
            String bucket,
            String objectKey,
            MinioStorageClient.StorageStatus status,
            String detailMessage,
            String etag,
            String versionId) {
        public boolean isSuccess() {
            return status == MinioStorageClient.StorageStatus.SUCCESS;
        }

        public boolean isSkipped() {
            return status == MinioStorageClient.StorageStatus.SKIPPED;
        }

        public boolean isFailure() {
            return status == MinioStorageClient.StorageStatus.FAILURE;
        }
    }
}
