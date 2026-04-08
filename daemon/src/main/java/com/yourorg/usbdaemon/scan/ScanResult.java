package com.yourorg.usbdaemon.scan;

import java.nio.file.Path;
import java.util.List;

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

    public ScanResult(Status status, Path mountPath, Path targetPath, List<Path> pcapFiles, String detailMessage) {
        this.status = status;
        this.mountPath = mountPath;
        this.targetPath = targetPath;
        this.pcapFiles = List.copyOf(pcapFiles);
        this.detailMessage = detailMessage;
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
}
