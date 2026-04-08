package com.yourorg.usbdaemon.config;

import java.nio.file.Path;
import java.util.Objects;

public final class AppConfig {
    private final String minioEndpoint;
    private final String minioAccessKey;
    private final String minioSecretKey;
    private final String ingestBucket;
    private final Path deviceMountPath;
    private final String scanRelativePath;
    private final int mountPathRetryCount;
    private final long mountPathRetryIntervalMillis;
    private final Path logPath;

    public AppConfig(
            String minioEndpoint,
            String minioAccessKey,
            String minioSecretKey,
            String ingestBucket,
            Path deviceMountPath,
            String scanRelativePath,
            int mountPathRetryCount,
            long mountPathRetryIntervalMillis,
            Path logPath) {
        this.minioEndpoint = requireText(minioEndpoint, "minioEndpoint");
        this.minioAccessKey = requireText(minioAccessKey, "minioAccessKey");
        this.minioSecretKey = requireText(minioSecretKey, "minioSecretKey");
        this.ingestBucket = requireText(ingestBucket, "ingestBucket");
        this.deviceMountPath = Objects.requireNonNull(deviceMountPath, "deviceMountPath");
        this.scanRelativePath = Objects.requireNonNull(scanRelativePath, "scanRelativePath");
        this.mountPathRetryCount = mountPathRetryCount;
        this.mountPathRetryIntervalMillis = mountPathRetryIntervalMillis;
        this.logPath = Objects.requireNonNull(logPath, "logPath");
    }

    public String getMinioEndpoint() {
        return minioEndpoint;
    }

    public String getMinioAccessKey() {
        return minioAccessKey;
    }

    public String getMinioSecretKey() {
        return minioSecretKey;
    }

    public String getIngestBucket() {
        return ingestBucket;
    }

    public Path getDeviceMountPath() {
        return deviceMountPath;
    }

    public String getScanRelativePath() {
        return scanRelativePath;
    }

    public int getMountPathRetryCount() {
        return mountPathRetryCount;
    }

    public long getMountPathRetryIntervalMillis() {
        return mountPathRetryIntervalMillis;
    }

    public Path getLogPath() {
        return logPath;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
