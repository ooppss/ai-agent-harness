package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MountPathResolver {
    private static final Path PROC_MOUNTS = Path.of("/proc/self/mounts");
    private static final Path PROC_MOUNTINFO = Path.of("/proc/self/mountinfo");

    @FunctionalInterface
    public interface MountTableReader {
        List<MountEntry> readMountEntries() throws IOException;
    }

    private final AppConfig config;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;
    private final MountTableReader mountTableReader;

    public MountPathResolver(AppConfig config, DaemonLogger daemonLogger, ErrorLogger errorLogger) {
        this(config, daemonLogger, errorLogger, null);
    }

    MountPathResolver(AppConfig config, DaemonLogger daemonLogger, ErrorLogger errorLogger, MountTableReader mountTableReader) {
        this.config = config;
        this.daemonLogger = Objects.requireNonNull(daemonLogger, "daemonLogger");
        this.errorLogger = Objects.requireNonNull(errorLogger, "errorLogger");
        this.mountTableReader = mountTableReader != null ? mountTableReader : this::readMountEntriesFromSystem;
    }

    public Optional<Path> resolveConfiguredMountPath() {
        return resolveMountPath(null);
    }

    public Optional<Path> resolveMountPath(String deviceName) {
        Path configuredPath = config.getDeviceMountPath().normalize();
        for (int attempt = 0; attempt <= config.getMountPathRetryCount(); attempt++) {
            Optional<Path> resolvedMountPath = resolveFromMountedFileSystems(configuredPath, deviceName);
            if (resolvedMountPath.isPresent()) {
                return resolvedMountPath;
            }
            if (attempt < config.getMountPathRetryCount()) {
                daemonLogger.logMountPathRetry(configuredPath, deviceName, attempt + 1, config.getMountPathRetryCount() + 1);
                sleepBeforeRetry();
            }
        }

        if (deviceName == null || deviceName.isBlank()) {
            errorLogger.logMountPathFailure(configuredPath, null, "Mount path could not be resolved");
        } else {
            errorLogger.logMountPathFailure(configuredPath, deviceName, "Mount path could not be resolved");
        }
        return Optional.empty();
    }

    private Optional<Path> resolveFromMountedFileSystems(Path configuredPath, String deviceName) {
        List<MountEntry> mountEntries = readMountEntries();
        if (mountEntries.isEmpty()) {
            return Optional.empty();
        }

        if (deviceName != null && !deviceName.isBlank()) {
            Optional<Path> matchedByDevice = mountEntries.stream()
                    .filter(entry -> entry.matchesDevice(deviceName))
                    .map(MountEntry::mountPoint)
                    .filter(this::isDirectory)
                    .filter(path -> matchesConfiguredPath(path, configuredPath))
                    .min(Comparator.comparingInt(path -> path.getNameCount()));
            if (matchedByDevice.isPresent()) {
                return matchedByDevice;
            }
        }

        return mountEntries.stream()
                .map(MountEntry::mountPoint)
                .filter(this::isDirectory)
                .filter(path -> matchesConfiguredPath(path, configuredPath))
                .min(Comparator.comparingInt(path -> path.getNameCount()));
    }

    private List<MountEntry> readMountEntries() {
        try {
            return List.copyOf(mountTableReader.readMountEntries());
        } catch (IOException exception) {
            errorLogger.logMountPathFailure(config.getDeviceMountPath(), deviceNameOrUnknown(), "Failed to read mount table from " + PROC_MOUNTS);
            return List.of();
        }
    }

    private List<MountEntry> readMountEntriesFromSystem() throws IOException {
        if (Files.isRegularFile(PROC_MOUNTINFO)) {
            List<MountEntry> mountInfoEntries = readMountInfoEntries();
            if (!mountInfoEntries.isEmpty()) {
                return mountInfoEntries;
            }
        }
        if (Files.isRegularFile(PROC_MOUNTS)) {
            return readProcMountEntries();
        }
        return List.of();
    }

    private List<MountEntry> readMountInfoEntries() throws IOException {
        List<MountEntry> entries = new ArrayList<>();
        for (String line : Files.readAllLines(PROC_MOUNTINFO, StandardCharsets.UTF_8)) {
            String[] separatorSplit = line.split(" - ", 2);
            if (separatorSplit.length != 2) {
                continue;
            }

            String[] left = separatorSplit[0].split(" ");
            String[] right = separatorSplit[1].split(" ");
            if (left.length < 5 || right.length < 2) {
                continue;
            }

            String mountPoint = decodeMountToken(left[4]);
            String source = decodeMountToken(right[1]);
            entries.add(new MountEntry(source, Paths.get(mountPoint).normalize()));
        }
        return List.copyOf(entries);
    }

    private List<MountEntry> readProcMountEntries() throws IOException {
        List<MountEntry> entries = new ArrayList<>();
        for (String line : Files.readAllLines(PROC_MOUNTS, StandardCharsets.UTF_8)) {
            String[] tokens = line.split(" ");
            if (tokens.length < 2) {
                continue;
            }
            String source = decodeMountToken(tokens[0]);
            String mountPoint = decodeMountToken(tokens[1]);
            entries.add(new MountEntry(source, Paths.get(mountPoint).normalize()));
        }
        return List.copyOf(entries);
    }

    private String deviceNameOrUnknown() {
        return "unknown";
    }

    private boolean matchesConfiguredPath(Path mountPath, Path configuredPath) {
        return mountPath.equals(configuredPath) || mountPath.startsWith(configuredPath) || configuredPath.startsWith(mountPath);
    }

    private boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    private String decodeMountToken(String token) {
        return token
                .replace("\\040", " ")
                .replace("\\011", "\t")
                .replace("\\012", "\n")
                .replace("\\134", "\\");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(config.getMountPathRetryIntervalMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            errorLogger.logMountPathFailure(config.getDeviceMountPath(), null, "Mount path retry interrupted");
        }
    }

    public record MountEntry(String source, Path mountPoint) {
        private boolean matchesDevice(String deviceName) {
            String normalizedDevice = deviceName.startsWith("/dev/") ? deviceName : "/dev/" + deviceName;
            return source.equals(deviceName)
                    || source.equals(normalizedDevice)
                    || source.startsWith(deviceName + "/")
                    || source.startsWith(normalizedDevice + "/")
                    || source.startsWith(deviceName + "p");
        }
    }
}
