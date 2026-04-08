package com.yourorg.usbdaemon.logging;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class DaemonLogger {
    private final Logger logger = Logger.getLogger(DaemonLogger.class.getName());

    public void logStartup(Path logPath) {
        logger.info(() -> "Daemon bootstrap started. logPath=" + logPath);
    }

    public void logDeviceScanStart(Path mountPath, Path targetPath) {
        logger.info(() -> "Starting pcap scan. mountPath=" + mountPath + ", targetPath=" + targetPath);
    }

    public void logDeviceScanFinish(long fileCount) {
        logger.info(() -> "Finished pcap scan. fileCount=" + fileCount);
    }

    public void logUploadStart(Path source, String bucket, String objectKey) {
        logger.info(() -> "Starting upload. source=" + source + ", bucket=" + bucket + ", objectKey=" + objectKey);
    }

    public void logUploadFinish(Path source, String bucket, String objectKey, boolean success) {
        logger.info(() -> "Finished upload. source=" + source + ", bucket=" + bucket + ", objectKey=" + objectKey + ", success=" + success);
    }

    public void logShutdown() {
        logger.info("Daemon bootstrap finished.");
    }
}
