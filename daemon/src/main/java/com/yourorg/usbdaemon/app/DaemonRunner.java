package com.yourorg.usbdaemon.app;

import com.yourorg.usbdaemon.device.DeviceEventListener;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.scan.PcapScanner;
import com.yourorg.usbdaemon.scan.ScanResult;
import com.yourorg.usbdaemon.storage.IngestUploader;

import java.nio.file.Path;
import java.util.Optional;

public final class DaemonRunner {
    private final DeviceEventListener deviceEventListener;
    private final PcapScanner pcapScanner;
    private final IngestUploader ingestUploader;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;

    public DaemonRunner(
            DeviceEventListener deviceEventListener,
            PcapScanner pcapScanner,
            IngestUploader ingestUploader,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger) {
        this.deviceEventListener = deviceEventListener;
        this.pcapScanner = pcapScanner;
        this.ingestUploader = ingestUploader;
        this.daemonLogger = daemonLogger;
        this.errorLogger = errorLogger;
    }

    public void runOnce() {
        Optional<Path> mountPath = deviceEventListener.awaitNextMountPath();
        if (mountPath.isEmpty()) {
            errorLogger.logFailure("No mount path resolved for ingest cycle");
            return;
        }

        ScanResult scanResult = pcapScanner.scan(mountPath.get());
        if (!scanResult.isSuccessful()) {
            errorLogger.logScanFailure(scanResult.getTargetPath(), scanResult.getDetailMessage());
            daemonLogger.logShutdown();
            return;
        }
        IngestUploader.UploadBatchResult uploadBatchResult = ingestUploader.upload(scanResult);
        if (!uploadBatchResult.isSuccessful()) {
            errorLogger.logFailure("Upload batch completed with failures: " + uploadBatchResult.detailMessage());
        }
        daemonLogger.logShutdown();
    }
}
