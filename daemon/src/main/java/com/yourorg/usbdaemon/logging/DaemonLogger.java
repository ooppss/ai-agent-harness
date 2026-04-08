package com.yourorg.usbdaemon.logging;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class DaemonLogger {
    private final Logger logger = Logger.getLogger(DaemonLogger.class.getName());

    public void logStartup(Path logPath) {
        logger.info(() -> "Daemon bootstrap started. logPath=" + logPath);
    }

    public void logDeviceEventDetected(String deviceName) {
        logger.info(() -> "Detected block device event. deviceName=" + deviceName);
    }

    public void logMountPathRetry(Path configuredPath, String deviceName, int attemptNumber, int maxAttempts) {
        logger.info(() -> "Retrying mount path resolution. configuredPath=" + configuredPath
                + ", deviceName=" + deviceName
                + ", attempt=" + attemptNumber
                + ", maxAttempts=" + maxAttempts);
    }

    public void logMountPathResolved(String deviceName, Path mountPath) {
        logger.info(() -> "Resolved mount path. deviceName=" + deviceName + ", mountPath=" + mountPath);
    }

    public void logScanStart(Path mountPath, Path targetPath) {
        logger.info(() -> "Starting pcap scan. mountPath=" + mountPath + ", targetPath=" + targetPath);
    }

    public void logScanFinish(Path mountPath, Path targetPath, long fileCount, int scannedDirectoryCount, String status) {
        logger.info(() -> "Finished pcap scan. mountPath=" + mountPath
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

    public void logUploadBatchFinish(String bucket, int totalCount, int successCount, int failureCount, String detailMessage) {
        logger.info(() -> "Finished upload batch. bucket=" + bucket
                + ", totalCount=" + totalCount
                + ", successCount=" + successCount
                + ", failureCount=" + failureCount
                + ", detail=" + detailMessage);
    }

    public void logShutdown() {
        logger.info("Daemon bootstrap finished.");
    }
}
