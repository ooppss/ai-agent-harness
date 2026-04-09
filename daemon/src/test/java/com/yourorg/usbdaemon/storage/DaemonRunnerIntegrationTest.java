package com.yourorg.usbdaemon.storage;

import com.yourorg.usbdaemon.app.DaemonRunner;
import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.device.DeviceEventListener;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.naming.ObjectKeyBuilder;
import com.yourorg.usbdaemon.scan.PcapScanner;
import com.yourorg.usbdaemon.scan.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaemonRunnerIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void runsDeviceScanStorageFlowAndBuildsExpectedUploadRequests() throws Exception {
        Path storagePath = Files.createDirectory(tempDir.resolve("mount-device"));
        Path vehicleDirectory = Files.createDirectories(storagePath.resolve("U100/20260327/U100-009"));
        Path firstPcap = writeFile(vehicleDirectory.resolve("cam_a_1_20260327112330.pcap"));
        Path secondPcap = writeFile(vehicleDirectory.resolve("imu_20260327112331.pcap"));
        writeFile(vehicleDirectory.resolve("notes.txt"));

        UploadRecorder uploadRecorder = new UploadRecorder();
        DaemonRunner daemonRunner = createRunner(storagePath, uploadRecorder);

        DaemonRunner.RunCycleResult runCycleResult = daemonRunner.runOnceForResult();

        assertTrue(runCycleResult.storagePath().isPresent());
        assertEquals(storagePath, runCycleResult.storagePath().get());
        assertTrue(runCycleResult.scanResult().isPresent());
        ScanResult scanResult = runCycleResult.scanResult().get();
        assertTrue(scanResult.isSuccessful());
        assertEquals(List.of(firstPcap, secondPcap), scanResult.getPcapFiles());
        assertTrue(runCycleResult.uploadBatchResult().isPresent());
        IngestUploader.UploadBatchResult uploadBatchResult = runCycleResult.uploadBatchResult().get();
        assertTrue(uploadBatchResult.isSuccessful());
        assertEquals(2, uploadBatchResult.totalCount());
        assertEquals(2, uploadBatchResult.successCount());
        assertEquals(0, uploadBatchResult.skippedCount());
        assertEquals(
                List.of(
                        "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap",
                        "U100/2026/03/27/260327_v009/imu_20260327112331.pcap"),
                uploadRecorder.objectKeys());
        assertEquals(List.of(firstPcap, secondPcap), uploadRecorder.sourceFiles());
    }

    @Test
    void keepsProcessingFlowAndReturnsSkippedDuplicatesOnLaterCycle() throws Exception {
        Path storagePath = Files.createDirectory(tempDir.resolve("mount-device"));
        Path vehicleDirectory = Files.createDirectories(storagePath.resolve("U100/20260327/U100-009"));
        writeFile(vehicleDirectory.resolve("cam_a_1_20260327112330.pcap"));

        UploadRecorder uploadRecorder = new UploadRecorder();
        DaemonRunner daemonRunner = createRunner(storagePath, uploadRecorder);

        DaemonRunner.RunCycleResult firstRun = daemonRunner.runOnceForResult();
        DaemonRunner.RunCycleResult secondRun = daemonRunner.runOnceForResult();

        IngestUploader.UploadBatchResult firstBatch = firstRun.uploadBatchResult().orElseThrow();
        IngestUploader.UploadBatchResult secondBatch = secondRun.uploadBatchResult().orElseThrow();

        assertEquals(1, firstBatch.successCount());
        assertEquals(0, firstBatch.skippedCount());
        assertEquals(0, firstBatch.failureCount());
        assertEquals(0, secondBatch.successCount());
        assertEquals(1, secondBatch.skippedCount());
        assertEquals(0, secondBatch.failureCount());
        assertTrue(secondBatch.isSuccessful());
        assertTrue(secondBatch.results().get(0).isSkipped());
        assertEquals(
                List.of(
                        "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap",
                        "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap"),
                uploadRecorder.objectKeys());
    }

    private DaemonRunner createRunner(Path storagePath, UploadRecorder uploadRecorder) {
        AppConfig appConfig = new AppConfig(
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "ingest-staging",
                storagePath,
                "",
                0,
                0,
                tempDir.resolve("daemon.log"));
        DaemonLogger daemonLogger = new DaemonLogger();
        ErrorLogger errorLogger = new ErrorLogger();
        DeviceEventListener deviceEventListener = new DeviceEventListener(
                null,
                daemonLogger,
                errorLogger,
                () -> {
                    daemonLogger.logStoragePathReady("test-stub", storagePath);
                    return java.util.Optional.of(storagePath);
                });
        PcapScanner pcapScanner = new PcapScanner(appConfig, daemonLogger, errorLogger);
        IngestUploader ingestUploader = new IngestUploader(
                appConfig,
                uploadRecorder::upload,
                new ObjectKeyBuilder(),
                daemonLogger,
                errorLogger);
        return new DaemonRunner(deviceEventListener, pcapScanner, ingestUploader, daemonLogger, errorLogger);
    }

    private Path writeFile(Path path) throws Exception {
        return Files.writeString(path, "pcap");
    }

    private static final class UploadRecorder {
        private final List<Path> sourceFiles = new ArrayList<>();
        private final List<String> objectKeys = new ArrayList<>();
        private final Set<String> uploadedKeys = new LinkedHashSet<>();

        private MinioStorageClient.StorageResponse upload(Path sourceFile, String bucket, String objectKey) {
            sourceFiles.add(sourceFile);
            objectKeys.add(objectKey);
            if (!uploadedKeys.add(objectKey)) {
                return new MinioStorageClient.StorageResponse(
                        bucket,
                        objectKey,
                        MinioStorageClient.StorageStatus.SKIPPED,
                        "Object already exists. Upload skipped",
                        null,
                        null);
            }
            return new MinioStorageClient.StorageResponse(
                    bucket,
                    objectKey,
                    MinioStorageClient.StorageStatus.SUCCESS,
                    "Upload completed",
                    "etag-" + sourceFiles.size(),
                    null);
        }

        private List<Path> sourceFiles() {
            return List.copyOf(sourceFiles);
        }

        private List<String> objectKeys() {
            return List.copyOf(objectKeys);
        }
    }
}
