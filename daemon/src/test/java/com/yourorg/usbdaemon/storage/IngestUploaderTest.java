package com.yourorg.usbdaemon.storage;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.naming.ObjectKeyBuilder;
import com.yourorg.usbdaemon.scan.ScanResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestUploaderTest {
    @Test
    void buildsUploadRequestFromScanResultAndObjectKeyBuilder() {
        AppConfig appConfig = testConfig();
        AtomicReference<Path> uploadedSource = new AtomicReference<>();
        AtomicReference<String> uploadedBucket = new AtomicReference<>();
        AtomicReference<String> uploadedObjectKey = new AtomicReference<>();

        IngestUploader ingestUploader = new IngestUploader(
                appConfig,
                (sourceFile, bucket, objectKey) -> {
                    uploadedSource.set(sourceFile);
                    uploadedBucket.set(bucket);
                    uploadedObjectKey.set(objectKey);
                    return new MinioStorageClient.StorageResponse(bucket, objectKey, true, "ok", "etag-1", null);
                },
                new ObjectKeyBuilder(),
                new DaemonLogger(),
                new ErrorLogger());

        Path mountPath = Path.of("/mnt/storage-device");
        Path pcapFile = mountPath.resolve("U100/20260327/U100-009/cam_a_1_20260327112330.pcap");
        ScanResult scanResult = new ScanResult(
                ScanResult.Status.SUCCESS,
                mountPath,
                mountPath.resolve("U100/20260327/U100-009"),
                List.of(pcapFile),
                "pcap files found",
                1);

        IngestUploader.UploadBatchResult result = ingestUploader.upload(scanResult);

        assertTrue(result.isSuccessful());
        assertEquals(1, result.totalCount());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(pcapFile, uploadedSource.get());
        assertEquals("ingest-staging", uploadedBucket.get());
        assertEquals(
                "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap",
                uploadedObjectKey.get());
    }

    @Test
    void reportsPartialFailureWhenOneUploadFails() {
        AppConfig appConfig = testConfig();
        AtomicReference<Integer> invocationCount = new AtomicReference<>(0);

        IngestUploader ingestUploader = new IngestUploader(
                appConfig,
                (sourceFile, bucket, objectKey) -> {
                    int current = invocationCount.get();
                    invocationCount.set(current + 1);
                    if (current == 0) {
                        return new MinioStorageClient.StorageResponse(bucket, objectKey, true, "ok", "etag-1", null);
                    }
                    return new MinioStorageClient.StorageResponse(bucket, objectKey, false, "upload failed", null, null);
                },
                new ObjectKeyBuilder(),
                new DaemonLogger(),
                new ErrorLogger());

        Path mountPath = Path.of("/mnt/storage-device");
        Path firstFile = mountPath.resolve("U100/20260327/U100-009/cam_a_1_20260327112330.pcap");
        Path secondFile = mountPath.resolve("U100/20260327/U100-009/imu_20260327112331.pcap");
        ScanResult scanResult = new ScanResult(
                ScanResult.Status.SUCCESS,
                mountPath,
                mountPath.resolve("U100/20260327/U100-009"),
                List.of(firstFile, secondFile),
                "pcap files found",
                1);

        IngestUploader.UploadBatchResult result = ingestUploader.upload(scanResult);

        assertFalse(result.isSuccessful());
        assertEquals(2, result.totalCount());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(2, result.results().size());
        assertTrue(result.results().get(0).success());
        assertFalse(result.results().get(1).success());
    }

    @Test
    void skipsUploadWhenScanResultIsNotSuccessful() {
        AppConfig appConfig = testConfig();
        AtomicReference<Boolean> uploadCalled = new AtomicReference<>(false);

        IngestUploader ingestUploader = new IngestUploader(
                appConfig,
                (sourceFile, bucket, objectKey) -> {
                    uploadCalled.set(true);
                    return new MinioStorageClient.StorageResponse(bucket, objectKey, true, "ok", "etag-1", null);
                },
                new ObjectKeyBuilder(),
                new DaemonLogger(),
                new ErrorLogger());

        ScanResult scanResult = new ScanResult(
                ScanResult.Status.NO_PCAP_FILES,
                Path.of("/mnt/storage-device"),
                Path.of("/mnt/storage-device/U100/20260327/U100-009"),
                List.of(),
                "No pcap files were found under target path",
                1);

        IngestUploader.UploadBatchResult result = ingestUploader.upload(scanResult);

        assertFalse(uploadCalled.get());
        assertTrue(result.isSuccessful());
        assertEquals(0, result.totalCount());
        assertEquals("Upload skipped because scan result was not successful", result.detailMessage());
    }

    private AppConfig testConfig() {
        return new AppConfig(
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "ingest-staging",
                Path.of("/mnt/storage-device"),
                "",
                3,
                1000,
                Path.of("/var/log/usbdaemon/daemon.log"));
    }
}
