package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountPathResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesMountInfoLinesIncludingEscapedMountPath() {
        List<MountPathResolver.MountEntry> entries = MountPathResolver.parseMountInfoLines(List.of(
                "29 23 8:17 / /media/My\\040Drive rw,nosuid,nodev - vfat /dev/sdb1 rw"));

        assertEquals(1, entries.size());
        assertEquals("/dev/sdb1", entries.get(0).source());
        assertEquals(Path.of("/media/My Drive"), entries.get(0).mountPoint());
    }

    @Test
    void parsesProcMountLinesIncludingEscapedMountPath() {
        List<MountPathResolver.MountEntry> entries = MountPathResolver.parseProcMountLines(List.of(
                "/dev/sdc1 /run/media/USB\\040DEVICE vfat rw 0 0"));

        assertEquals(1, entries.size());
        assertEquals("/dev/sdc1", entries.get(0).source());
        assertEquals(Path.of("/run/media/USB DEVICE"), entries.get(0).mountPoint());
    }

    @Test
    void resolvesMountPathForBareDeviceName() throws Exception {
        Path configuredPath = tempDir.resolve("mnt");
        Path mountPath = configuredPath.resolve("usb-01");
        Files.createDirectories(mountPath);

        MountPathResolver resolver = new MountPathResolver(
                testConfig(configuredPath),
                new DaemonLogger(),
                new ErrorLogger(),
                () -> List.of(new MountPathResolver.MountEntry("/dev/sdb1", mountPath)));

        Optional<Path> resolved = resolver.resolveMountPath("sdb1");

        assertTrue(resolved.isPresent());
        assertEquals(mountPath, resolved.get());
    }

    @Test
    void fallsBackToConfiguredPathCompatibleMountWhenDeviceDoesNotMatch() throws Exception {
        Path configuredPath = tempDir.resolve("mnt");
        Path mountPath = configuredPath.resolve("usb-02");
        Files.createDirectories(mountPath);

        MountPathResolver resolver = new MountPathResolver(
                testConfig(configuredPath),
                new DaemonLogger(),
                new ErrorLogger(),
                () -> List.of(new MountPathResolver.MountEntry("/dev/sdz1", mountPath)));

        Optional<Path> resolved = resolver.resolveMountPath("/dev/sdb1");

        assertTrue(resolved.isPresent());
        assertEquals(mountPath, resolved.get());
    }

    @Test
    void returnsEmptyWhenMountTableReaderFails() {
        Path configuredPath = tempDir.resolve("mnt");
        MountPathResolver resolver = new MountPathResolver(
                testConfig(configuredPath),
                new DaemonLogger(),
                new ErrorLogger(),
                () -> {
                    throw new IOException("mount table unavailable");
                });

        Optional<Path> resolved = resolver.resolveConfiguredMountPath();

        assertFalse(resolved.isPresent());
    }

    private AppConfig testConfig(Path mountPath) {
        return new AppConfig(
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "ingest-staging",
                mountPath,
                "",
                0,
                0,
                tempDir.resolve("daemon.log"));
    }
}
