package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.logging.DaemonLogger;

import java.nio.file.Path;
import java.util.Optional;

public final class DeviceEventListener {
    private final MountPathResolver mountPathResolver;
    private final DaemonLogger daemonLogger;

    public DeviceEventListener(MountPathResolver mountPathResolver, DaemonLogger daemonLogger) {
        this.mountPathResolver = mountPathResolver;
        this.daemonLogger = daemonLogger;
    }

    public Optional<Path> awaitNextMountPath() {
        Optional<Path> mountPath = mountPathResolver.resolveConfiguredMountPath();
        mountPath.ifPresent(path -> daemonLogger.logDeviceScanStart(path, path));
        return mountPath;
    }
}
