package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.logging.DaemonLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DeviceEventListener {
    @FunctionalInterface
    public interface MountPathAwaiter {
        Optional<Path> awaitNextMountPath();
    }

    private final MountPathResolver mountPathResolver;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;
    private final MountPathAwaiter mountPathAwaiter;

    public DeviceEventListener(
            MountPathResolver mountPathResolver,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger) {
        this(mountPathResolver, daemonLogger, errorLogger, null);
    }

    public DeviceEventListener(
            MountPathResolver mountPathResolver,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger,
            MountPathAwaiter mountPathAwaiter) {
        this.mountPathResolver = mountPathResolver;
        this.daemonLogger = daemonLogger;
        this.errorLogger = errorLogger;
        this.mountPathAwaiter = mountPathAwaiter;
    }

    public Optional<Path> awaitNextMountPath() {
        if (mountPathAwaiter != null) {
            return mountPathAwaiter.awaitNextMountPath();
        }

        Optional<Path> mountPath = mountPathResolver.resolveConfiguredMountPath();
        if (mountPath.isPresent()) {
            daemonLogger.logMountPathResolved("configured-path", mountPath.get());
            return mountPath;
        }

        Optional<String> deviceName = awaitNextDeviceNameFromUdev();
        if (deviceName.isEmpty()) {
            return Optional.empty();
        }

        mountPath = mountPathResolver.resolveMountPath(deviceName.get());
        mountPath.ifPresent(path -> daemonLogger.logMountPathResolved(deviceName.get(), path));
        return mountPath;
    }

    private Optional<String> awaitNextDeviceNameFromUdev() {
        if (!isLinux()) {
            errorLogger.logFailure("udev device listening is only supported on Linux");
            return Optional.empty();
        }

        Process process = null;
        try {
            process = new ProcessBuilder("udevadm", "monitor", "--udev", "--subsystem-match=block", "--property")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                return readNextRelevantDevice(reader);
            }
        } catch (IOException exception) {
            errorLogger.logFailure("Failed to start udev monitor process", exception);
            return Optional.empty();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private Optional<String> readNextRelevantDevice(BufferedReader reader) throws IOException {
        Map<String, String> properties = new LinkedHashMap<>();
        String headerLine = null;
        String line;

        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                Optional<String> deviceName = resolveRelevantDevice(headerLine, properties);
                if (deviceName.isPresent()) {
                    return deviceName;
                }
                headerLine = null;
                properties.clear();
                continue;
            }

            if (trimmed.startsWith("UDEV")) {
                headerLine = trimmed;
                properties.clear();
                continue;
            }

            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex > 0) {
                properties.put(trimmed.substring(0, separatorIndex), trimmed.substring(separatorIndex + 1));
            }
        }

        return resolveRelevantDevice(headerLine, properties);
    }

    private Optional<String> resolveRelevantDevice(String headerLine, Map<String, String> properties) {
        if (headerLine == null || !headerLine.contains("(block)")) {
            return Optional.empty();
        }

        String action = properties.get("ACTION");
        if (!"add".equals(action) && !"bind".equals(action)) {
            return Optional.empty();
        }

        String deviceName = properties.get("DEVNAME");
        if (deviceName == null || deviceName.isBlank()) {
            return Optional.empty();
        }

        daemonLogger.logDeviceEventDetected(deviceName);
        return Optional.of(deviceName);
    }

    private boolean isLinux() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase().contains("linux");
    }
}
