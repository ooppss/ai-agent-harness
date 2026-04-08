package com.yourorg.usbdaemon.storage;

import com.yourorg.usbdaemon.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioStorageClientTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsFailureWhenBucketDoesNotExist() throws Exception {
        AtomicBoolean uploadCalled = new AtomicBoolean(false);
        MinioStorageClient storageClient = new MinioStorageClient(
                testConfig(),
                new MinioStorageClient.MinioGateway() {
                    @Override
                    public boolean bucketExists(String bucket) {
                        return false;
                    }

                    @Override
                    public boolean objectExists(String bucket, String objectKey) {
                        return false;
                    }

                    @Override
                    public MinioStorageClient.WriteResult upload(Path sourceFile, String bucket, String objectKey) {
                        uploadCalled.set(true);
                        return new MinioStorageClient.WriteResult("etag-1", null);
                    }
                });

        Path sourceFile = createPcapFile("cam_a_1_20260327112330.pcap");

        MinioStorageClient.StorageResponse response = storageClient.upload(
                sourceFile,
                "ingest-staging",
                "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap");

        assertTrue(response.isFailure());
        assertEquals("Bucket does not exist", response.detailMessage());
        assertFalse(uploadCalled.get());
    }

    @Test
    void skipsUploadWhenObjectAlreadyExists() throws Exception {
        AtomicBoolean uploadCalled = new AtomicBoolean(false);
        AtomicReference<String> checkedBucket = new AtomicReference<>();
        AtomicReference<String> checkedObjectKey = new AtomicReference<>();
        MinioStorageClient storageClient = new MinioStorageClient(
                testConfig(),
                new MinioStorageClient.MinioGateway() {
                    @Override
                    public boolean bucketExists(String bucket) {
                        checkedBucket.set(bucket);
                        return true;
                    }

                    @Override
                    public boolean objectExists(String bucket, String objectKey) {
                        checkedObjectKey.set(objectKey);
                        return true;
                    }

                    @Override
                    public MinioStorageClient.WriteResult upload(Path sourceFile, String bucket, String objectKey) {
                        uploadCalled.set(true);
                        return new MinioStorageClient.WriteResult("etag-1", null);
                    }
                });

        Path sourceFile = createPcapFile("cam_a_1_20260327112330.pcap");
        String objectKey = "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap";

        MinioStorageClient.StorageResponse response = storageClient.upload(sourceFile, "ingest-staging", objectKey);

        assertTrue(response.isSkipped());
        assertEquals("ingest-staging", checkedBucket.get());
        assertEquals(objectKey, checkedObjectKey.get());
        assertEquals("Object already exists. Upload skipped", response.detailMessage());
        assertFalse(uploadCalled.get());
    }

    @Test
    void uploadsObjectWhenBucketExistsAndObjectDoesNotExist() throws Exception {
        AtomicReference<Path> uploadedSource = new AtomicReference<>();
        AtomicReference<String> uploadedBucket = new AtomicReference<>();
        AtomicReference<String> uploadedObjectKey = new AtomicReference<>();
        MinioStorageClient storageClient = new MinioStorageClient(
                testConfig(),
                new MinioStorageClient.MinioGateway() {
                    @Override
                    public boolean bucketExists(String bucket) {
                        return true;
                    }

                    @Override
                    public boolean objectExists(String bucket, String objectKey) {
                        return false;
                    }

                    @Override
                    public MinioStorageClient.WriteResult upload(Path sourceFile, String bucket, String objectKey) {
                        uploadedSource.set(sourceFile);
                        uploadedBucket.set(bucket);
                        uploadedObjectKey.set(objectKey);
                        return new MinioStorageClient.WriteResult("etag-1", "version-1");
                    }
                });

        Path sourceFile = createPcapFile("imu_20260327112331.pcap");
        String objectKey = "U100/2026/03/27/260327_v009/imu_20260327112331.pcap";

        MinioStorageClient.StorageResponse response = storageClient.upload(sourceFile, "ingest-staging", objectKey);

        assertTrue(response.isSuccess());
        assertEquals("Upload completed", response.detailMessage());
        assertEquals("etag-1", response.etag());
        assertEquals("version-1", response.versionId());
        assertEquals(sourceFile, uploadedSource.get());
        assertEquals("ingest-staging", uploadedBucket.get());
        assertEquals(objectKey, uploadedObjectKey.get());
    }

    @Test
    void exposesBucketAndObjectExistenceChecksThroughGateway() throws Exception {
        MinioStorageClient storageClient = new MinioStorageClient(
                testConfig(),
                new MinioStorageClient.MinioGateway() {
                    @Override
                    public boolean bucketExists(String bucket) {
                        return "ingest-staging".equals(bucket);
                    }

                    @Override
                    public boolean objectExists(String bucket, String objectKey) {
                        return objectKey.endsWith("cam_a_1_20260327112330.pcap");
                    }

                    @Override
                    public MinioStorageClient.WriteResult upload(Path sourceFile, String bucket, String objectKey) {
                        return new MinioStorageClient.WriteResult("etag-1", null);
                    }
                });

        assertTrue(storageClient.bucketExists("ingest-staging"));
        assertFalse(storageClient.bucketExists("missing-bucket"));
        assertTrue(storageClient.objectExists("ingest-staging", "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap"));
        assertFalse(storageClient.objectExists("ingest-staging", "U100/2026/03/27/260327_v009/imu_20260327112331.pcap"));
    }

    private Path createPcapFile(String fileName) throws Exception {
        Path sourceFile = tempDir.resolve(fileName);
        Files.writeString(sourceFile, "pcap");
        return sourceFile;
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
