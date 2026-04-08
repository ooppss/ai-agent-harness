package com.yourorg.usbdaemon.scan;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ScanResult {
    public enum Status {
        SUCCESS,
        TARGET_DIRECTORY_MISSING,
        NO_PCAP_FILES,
        FAILED
    }

    private final Status status;
    private final Path mountPath;
    private final Path targetPath;
    private final List<Path> pcapFiles;
    private final String detailMessage;
    private final int scannedDirectoryCount;

    public ScanResult(
            Status status,
            Path mountPath,
            Path targetPath,
            List<Path> pcapFiles,
            String detailMessage,
            int scannedDirectoryCount) {
        this.status = Objects.requireNonNull(status, "status");
        this.mountPath = Objects.requireNonNull(mountPath, "mountPath");
        this.targetPath = Objects.requireNonNull(targetPath, "targetPath");
        this.pcapFiles = List.copyOf(pcapFiles);
        this.detailMessage = Objects.requireNonNull(detailMessage, "detailMessage");
        this.scannedDirectoryCount = scannedDirectoryCount;
    }

    public Status getStatus() {
        return status;
    }

    public Path getMountPath() {
        return mountPath;
    }

    public Path getTargetPath() {
        return targetPath;
    }

    public List<Path> getPcapFiles() {
        return pcapFiles;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public int getScannedDirectoryCount() {
        return scannedDirectoryCount;
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }
}
