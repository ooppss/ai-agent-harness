package com.yourorg.usbdaemon.naming;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectKeyBuilderTest {
    private final ObjectKeyBuilder objectKeyBuilder = new ObjectKeyBuilder();

    @Test
    void buildsObjectKeyFromDocumentedInputPath() {
        Path mountPath = Path.of("/mnt/storage-device");
        Path pcapFile = mountPath.resolve("U100/20260327/U100-009/cam_a_1_20260327112330.pcap");

        String objectKey = objectKeyBuilder.build(mountPath, pcapFile);

        assertEquals(
                "U100/2026/03/27/260327_v009/cam_a_1_20260327112330.pcap",
                objectKey);
    }

    @Test
    void buildsObjectKeyUsingTrailingDocumentedSegmentsWhenAdditionalPrefixExists() {
        Path mountPath = Path.of("/mnt/storage-device");
        Path pcapFile = mountPath.resolve("capture-root/archive/U100/20260327/U100-009/imu_20260327112331.pcap");

        String objectKey = objectKeyBuilder.build(mountPath, pcapFile);

        assertEquals(
                "U100/2026/03/27/260327_v009/imu_20260327112331.pcap",
                objectKey);
    }

    @Test
    void rejectsPathOutsideMountPath() {
        Path mountPath = Path.of("/mnt/storage-device");
        Path pcapFile = Path.of("/tmp/U100/20260327/U100-009/cam_a_1_20260327112330.pcap");

        assertThrows(IllegalArgumentException.class, () -> objectKeyBuilder.build(mountPath, pcapFile));
    }

    @Test
    void rejectsInvalidCollectionDateSegment() {
        Path mountPath = Path.of("/mnt/storage-device");
        Path pcapFile = mountPath.resolve("U100/notadate/U100-009/cam_a_1_20260327112330.pcap");

        assertThrows(IllegalArgumentException.class, () -> objectKeyBuilder.build(mountPath, pcapFile));
    }
}
