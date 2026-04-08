package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.ErrorLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class MountPathResolver {
    private final AppConfig config;
    private final ErrorLogger errorLogger;

    public MountPathResolver(AppConfig config, ErrorLogger errorLogger) {
        this.config = config;
        this.errorLogger = errorLogger;
    }

    public Optional<Path> resolveConfiguredMountPath() {
        Path configuredPath = config.getDeviceMountPath();
        for (int attempt = 0; attempt <= config.getMountPathRetryCount(); attempt++) {
            if (Files.isDirectory(configuredPath)) {
                return Optional.of(configuredPath);
            }
            if (attempt < config.getMountPathRetryCount()) {
                sleepBeforeRetry();
            }
        }
        errorLogger.logFailure("Mount path could not be resolved: " + configuredPath);
        return Optional.empty();
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(config.getMountPathRetryIntervalMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            errorLogger.logFailure("Mount path retry interrupted", exception);
        }
    }
}
