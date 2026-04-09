package com.yourorg.usbdaemon.scan;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class PcapScanner {
    private final AppConfig config;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;

    public PcapScanner(AppConfig config, DaemonLogger daemonLogger, ErrorLogger errorLogger) {
        this.config = config;
        this.daemonLogger = daemonLogger;
        this.errorLogger = errorLogger;
    }

    public ScanResult scan(Path storagePath) {
        Path targetPath = resolveTargetPath(storagePath);
        daemonLogger.logScanStart(storagePath, targetPath);

        if (!targetPath.startsWith(storagePath.normalize())) {
            errorLogger.logScanFailure(targetPath, "Resolved target path escapes storage path");
            return new ScanResult(
                    ScanResult.Status.FAILED,
                    storagePath,
                    targetPath,
                    List.of(),
                    "Resolved target path escapes storage path",
                    0);
        }

        if (!Files.isDirectory(targetPath)) {
            errorLogger.logScanFailure(targetPath, "Target scan directory does not exist");
            return new ScanResult(
                    ScanResult.Status.TARGET_DIRECTORY_MISSING,
                    storagePath,
                    targetPath,
                    List.of(),
                    "Target scan directory does not exist",
                    0);
        }

        try {
            ScanDiscovery discovery = config.getScanRelativePath().isBlank()
                    ? discoverByDocumentedStructure(targetPath)
                    : discoverFromConfiguredTarget(targetPath);

            ScanResult.Status status = discovery.pcapFiles().isEmpty()
                    ? ScanResult.Status.NO_PCAP_FILES
                    : ScanResult.Status.SUCCESS;
            daemonLogger.logScanFinish(
                    storagePath,
                    targetPath,
                    discovery.pcapFiles().size(),
                    discovery.scannedDirectoryCount(),
                    status.name());

            if (discovery.pcapFiles().isEmpty()) {
                errorLogger.logScanFailure(targetPath, "No pcap files were found under target path");
                return new ScanResult(
                    ScanResult.Status.NO_PCAP_FILES,
                    storagePath,
                    targetPath,
                    List.of(),
                    "No pcap files were found under target path",
                        discovery.scannedDirectoryCount());
            }

            return new ScanResult(
                    ScanResult.Status.SUCCESS,
                    storagePath,
                    targetPath,
                    discovery.pcapFiles(),
                    "pcap files found",
                    discovery.scannedDirectoryCount());
        } catch (IOException exception) {
            errorLogger.logScanFailure(targetPath, "Failed to scan pcap files under target path", exception);
            return new ScanResult(
                    ScanResult.Status.FAILED,
                    storagePath,
                    targetPath,
                    List.of(),
                    "Scan failed: " + exception.getMessage(),
                    0);
        }
    }

    private Path resolveTargetPath(Path storagePath) {
        String relativePath = config.getScanRelativePath();
        return relativePath.isBlank() ? storagePath : storagePath.resolve(relativePath).normalize();
    }

    private ScanDiscovery discoverFromConfiguredTarget(Path targetPath) throws IOException {
        try (Stream<Path> pathStream = Files.walk(targetPath)) {
            List<Path> pcapFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isPcapFile)
                    .sorted()
                    .toList();
            return new ScanDiscovery(pcapFiles, 1);
        }
    }

    private ScanDiscovery discoverByDocumentedStructure(Path storagePath) throws IOException {
        List<Path> candidateDirectories;
        try (Stream<Path> pathStream = Files.walk(storagePath, 3)) {
            candidateDirectories = pathStream
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(storagePath))
                    .filter(path -> storagePath.relativize(path).getNameCount() == 3)
                    .sorted()
                    .toList();
        }

        List<Path> pcapFiles = new ArrayList<>();
        for (Path candidateDirectory : candidateDirectories) {
            try (Stream<Path> childStream = Files.list(candidateDirectory)) {
                childStream
                        .filter(Files::isRegularFile)
                        .filter(this::isPcapFile)
                        .sorted()
                        .forEach(pcapFiles::add);
            }
        }

        return new ScanDiscovery(List.copyOf(pcapFiles), candidateDirectories.size());
    }

    private boolean isPcapFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".pcap");
    }

    private record ScanDiscovery(List<Path> pcapFiles, int scannedDirectoryCount) {
    }
}
