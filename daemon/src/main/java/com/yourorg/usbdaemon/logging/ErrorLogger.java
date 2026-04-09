package com.yourorg.usbdaemon.logging;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ErrorLogger {
    private final Logger logger = Logger.getLogger(ErrorLogger.class.getName());

    public void logFailure(String message) {
        logger.severe(message);
    }

    public void logFailure(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public void logDeviceListenerFailure(String message, Throwable throwable) {
        logger.log(Level.SEVERE, "Device listener failure: " + message, throwable);
    }

    public void logStoragePathUnavailable(Path configuredPath, String deviceName, String detailMessage) {
        logger.severe(() -> "File-browsable storage path confirmation failure. configuredPath=" + configuredPath
                + ", deviceName=" + deviceName
                + ", detail=" + detailMessage);
    }

    public void logScanFailure(Path targetPath, String detailMessage) {
        logger.severe(() -> "Scan failure. targetPath=" + targetPath + ", detail=" + detailMessage);
    }

    public void logScanFailure(Path targetPath, String detailMessage, Throwable throwable) {
        logger.log(Level.SEVERE, "Scan failure. targetPath=" + targetPath + ", detail=" + detailMessage, throwable);
    }

    public void logObjectKeyFailure(Path sourceFile, Throwable throwable) {
        logger.log(Level.SEVERE, "Object key build failure. source=" + sourceFile, throwable);
    }

    public void logUploadFailure(Path sourceFile, String bucket, String objectKey, String detailMessage) {
        logger.severe(() -> "Upload failure. source=" + sourceFile
                + ", bucket=" + bucket
                + ", objectKey=" + objectKey
                + ", detail=" + detailMessage);
    }

    public void logUploadFailure(Path sourceFile, String bucket, String objectKey, String detailMessage, Throwable throwable) {
        logger.log(Level.SEVERE,
                "Upload failure. source=" + sourceFile
                        + ", bucket=" + bucket
                        + ", objectKey=" + objectKey
                        + ", detail=" + detailMessage,
                throwable);
    }
}
