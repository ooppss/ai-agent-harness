package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceEventListenerTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsInjectedMountPathThroughTestSeam() {
        Path injectedMountPath = tempDir.resolve("mounted-device");
        DeviceEventListener listener = new DeviceEventListener(
                null,
                new DaemonLogger(),
                new ErrorLogger(),
                () -> Optional.of(injectedMountPath));

        Optional<Path> resolved = listener.awaitNextMountPath();

        assertTrue(resolved.isPresent());
        assertEquals(injectedMountPath, resolved.get());
    }

    @Test
    void fallsBackFromConfiguredPathLookupToDeviceNameResolution() throws Exception {
        Path configuredPath = tempDir.resolve("mnt");
        Path resolvedMountPath = configuredPath.resolve("usb-01");
        Files.createDirectories(resolvedMountPath);

        AtomicInteger readCount = new AtomicInteger();
        MountPathResolver resolver = new MountPathResolver(
                testConfig(configuredPath),
                new DaemonLogger(),
                new ErrorLogger(),
                () -> {
                    if (readCount.getAndIncrement() == 0) {
                        return List.of();
                    }
                    return List.of(new MountPathResolver.MountEntry("/dev/sdb1", resolvedMountPath));
                });

        AtomicBoolean deviceAwaiterCalled = new AtomicBoolean(false);
        DeviceEventListener listener = new DeviceEventListener(
                resolver,
                new DaemonLogger(),
                new ErrorLogger(),
                null,
                () -> {
                    deviceAwaiterCalled.set(true);
                    return Optional.of("/dev/sdb1");
                });

        Optional<Path> resolved = listener.awaitNextMountPath();

        assertTrue(deviceAwaiterCalled.get());
        assertTrue(resolved.isPresent());
        assertEquals(resolvedMountPath, resolved.get());
        assertEquals(2, readCount.get());
    }

    @Test
    void returnsEmptyWhenDeviceNameAwaiterDoesNotProduceEvent() {
        Path configuredPath = tempDir.resolve("mnt");
        MountPathResolver resolver = new MountPathResolver(
                testConfig(configuredPath),
                new DaemonLogger(),
                new ErrorLogger(),
                () -> List.of());

        DeviceEventListener listener = new DeviceEventListener(
                resolver,
                new DaemonLogger(),
                new ErrorLogger(),
                null,
                Optional::<String>empty);

        Optional<Path> resolved = listener.awaitNextMountPath();

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
