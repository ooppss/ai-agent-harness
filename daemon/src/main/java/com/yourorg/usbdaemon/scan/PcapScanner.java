package com.yourorg.usbdaemon.scan;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    public ScanResult scan(Path mountPath) {
        Path targetPath = resolveTargetPath(mountPath);
        daemonLogger.logDeviceScanStart(mountPath, targetPath);

        if (!Files.isDirectory(targetPath)) {
            return new ScanResult(
                    ScanResult.Status.TARGET_DIRECTORY_MISSING,
                    mountPath,
                    targetPath,
                    List.of(),
                    "Target scan directory does not exist");
        }

        try (Stream<Path> pathStream = Files.walk(targetPath)) {
            List<Path> pcapFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".pcap"))
                    .toList();

            daemonLogger.logDeviceScanFinish(pcapFiles.size());

            if (pcapFiles.isEmpty()) {
                return new ScanResult(
                        ScanResult.Status.NO_PCAP_FILES,
                        mountPath,
                        targetPath,
                        pcapFiles,
                        "No pcap files were found under target path");
            }

            return new ScanResult(
                    ScanResult.Status.SUCCESS,
                    mountPath,
                    targetPath,
                    pcapFiles,
                    "pcap files found");
        } catch (IOException exception) {
            errorLogger.logFailure("Failed to scan pcap files under " + targetPath, exception);
            return new ScanResult(
                    ScanResult.Status.FAILED,
                    mountPath,
                    targetPath,
                    List.of(),
                    "Scan failed: " + exception.getMessage());
        }
    }

    private Path resolveTargetPath(Path mountPath) {
        String relativePath = config.getScanRelativePath();
        return relativePath.isBlank() ? mountPath : mountPath.resolve(relativePath).normalize();
    }
}
