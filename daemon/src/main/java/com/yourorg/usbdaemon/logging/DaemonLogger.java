package com.yourorg.usbdaemon.logging;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class DaemonLogger {
    private final Logger logger = Logger.getLogger(DaemonLogger.class.getName());

    public void logStartup(Path logPath) {
        logger.info(() -> "Daemon bootstrap started. logPath=" + logPath);
    }

    public void logUsbStorageDetected(String deviceName) {
        logger.info(() -> "Detected USB storage add event candidate. deviceName=" + deviceName);
    }

    public void logStoragePathRetry(Path configuredPath, String deviceName, int attemptNumber, int maxAttempts) {
        logger.info(() -> "Retrying file-browsable storage path confirmation. configuredPath=" + configuredPath
                + ", deviceName=" + deviceName
                + ", attempt=" + attemptNumber
                + ", maxAttempts=" + maxAttempts);
    }

    public void logStoragePathReady(String deviceName, Path storagePath) {
        logger.info(() -> "Confirmed file-browsable storage path. deviceName=" + deviceName + ", storagePath=" + storagePath);
    }

    public void logScanStart(Path storagePath, Path targetPath) {
        logger.info(() -> "Starting pcap scan. storagePath=" + storagePath + ", targetPath=" + targetPath);
    }

    public void logScanFinish(Path storagePath, Path targetPath, long fileCount, int scannedDirectoryCount, String status) {
        logger.info(() -> "Finished pcap scan. storagePath=" + storagePath
                + ", targetPath=" + targetPath
                + ", fileCount=" + fileCount
                + ", scannedDirectoryCount=" + scannedDirectoryCount
                + ", status=" + status);
    }

    public void logObjectKeyBuilt(Path source, String objectKey) {
        logger.info(() -> "Built object key. source=" + source + ", objectKey=" + objectKey);
    }

    public void logUploadStart(Path source, String bucket, String objectKey) {
        logger.info(() -> "Starting upload. source=" + source + ", bucket=" + bucket + ", objectKey=" + objectKey);
    }

    public void logUploadFinish(Path source, String bucket, String objectKey, boolean success, String etag) {
        logger.info(() -> "Finished upload. source=" + source
                + ", bucket=" + bucket
                + ", objectKey=" + objectKey
                + ", success=" + success
                + ", etag=" + etag);
    }

    public void logUploadSkipped(Path source, String bucket, String objectKey, String detailMessage) {
        logger.info(() -> "Skipped upload. source=" + source
                + ", bucket=" + bucket
                + ", objectKey=" + objectKey
                + ", detail=" + detailMessage);
    }

    public void logUploadBatchFinish(String bucket, int totalCount, int successCount, int skippedCount, int failureCount, String detailMessage) {
        logger.info(() -> "Finished upload batch. bucket=" + bucket
                + ", totalCount=" + totalCount
                + ", successCount=" + successCount
                + ", skippedCount=" + skippedCount
                + ", failureCount=" + failureCount
                + ", detail=" + detailMessage);
    }

    public void logShutdown() {
        logger.info("Daemon bootstrap finished.");
    }
}
