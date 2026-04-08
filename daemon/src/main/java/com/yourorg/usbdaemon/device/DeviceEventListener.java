package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.sun.jna.Pointer;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class DeviceEventListener {
    @FunctionalInterface
    public interface MountPathAwaiter {
        Optional<Path> awaitNextMountPath();
    }

    @FunctionalInterface
    interface DeviceNameAwaiter {
        Optional<String> awaitNextDeviceName();
    }

    private final MountPathResolver mountPathResolver;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;
    private final MountPathAwaiter mountPathAwaiter;
    private final DeviceNameAwaiter deviceNameAwaiter;

    public DeviceEventListener(
            MountPathResolver mountPathResolver,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger) {
        this(mountPathResolver, daemonLogger, errorLogger, null, null);
    }

    public DeviceEventListener(
            MountPathResolver mountPathResolver,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger,
            MountPathAwaiter mountPathAwaiter) {
        this(mountPathResolver, daemonLogger, errorLogger, mountPathAwaiter, null);
    }

    DeviceEventListener(
            MountPathResolver mountPathResolver,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger,
            MountPathAwaiter mountPathAwaiter,
            DeviceNameAwaiter deviceNameAwaiter) {
        this.mountPathResolver = mountPathResolver;
        this.daemonLogger = Objects.requireNonNull(daemonLogger, "daemonLogger");
        this.errorLogger = Objects.requireNonNull(errorLogger, "errorLogger");
        this.mountPathAwaiter = mountPathAwaiter;
        this.deviceNameAwaiter = deviceNameAwaiter != null
                ? deviceNameAwaiter
                : new LibUdevDeviceNameAwaiter(this.daemonLogger, this.errorLogger);
    }

    public Optional<Path> awaitNextMountPath() {
        if (mountPathAwaiter != null) {
            return mountPathAwaiter.awaitNextMountPath();
        }

        if (mountPathResolver == null) {
            errorLogger.logFailure("MountPathResolver is required for operational device listening");
            return Optional.empty();
        }

        Optional<Path> mountPath = mountPathResolver.resolveConfiguredMountPath();
        if (mountPath.isPresent()) {
            daemonLogger.logMountPathResolved("configured-path", mountPath.get());
            return mountPath;
        }

        Optional<String> deviceName = deviceNameAwaiter.awaitNextDeviceName();
        if (deviceName.isEmpty()) {
            return Optional.empty();
        }

        mountPath = mountPathResolver.resolveMountPath(deviceName.get());
        mountPath.ifPresent(path -> daemonLogger.logMountPathResolved(deviceName.get(), path));
        return mountPath;
    }

    private static final class LibUdevDeviceNameAwaiter implements DeviceNameAwaiter {
        private static final long POLL_INTERVAL_MILLIS = 200L;

        private final DaemonLogger daemonLogger;
        private final ErrorLogger errorLogger;

        private LibUdevDeviceNameAwaiter(DaemonLogger daemonLogger, ErrorLogger errorLogger) {
            this.daemonLogger = daemonLogger;
            this.errorLogger = errorLogger;
        }

        @Override
        public Optional<String> awaitNextDeviceName() {
            if (!isLinux()) {
                errorLogger.logFailure("libudev device listening is only supported on Linux");
                return Optional.empty();
            }

            Pointer udev = null;
            Pointer monitor = null;
            try {
                udev = LibUdev.INSTANCE.udev_new();
                if (udev == null) {
                    errorLogger.logFailure("Failed to initialize libudev context");
                    return Optional.empty();
                }

                monitor = LibUdev.INSTANCE.udev_monitor_new_from_netlink(udev, "udev");
                if (monitor == null) {
                    errorLogger.logFailure("Failed to create libudev monitor");
                    return Optional.empty();
                }

                if (LibUdev.INSTANCE.udev_monitor_filter_add_match_subsystem_devtype(monitor, "block", null) != 0) {
                    errorLogger.logFailure("Failed to configure libudev block filter");
                    return Optional.empty();
                }

                if (LibUdev.INSTANCE.udev_monitor_enable_receiving(monitor) != 0) {
                    errorLogger.logFailure("Failed to enable libudev monitor receiving");
                    return Optional.empty();
                }

                while (!Thread.currentThread().isInterrupted()) {
                    Pointer device = LibUdev.INSTANCE.udev_monitor_receive_device(monitor);
                    if (device == null) {
                        sleepQuietly();
                        continue;
                    }

                    try {
                        Optional<String> deviceName = resolveRelevantDevice(device);
                        if (deviceName.isPresent()) {
                            daemonLogger.logDeviceEventDetected(deviceName.get());
                            return deviceName;
                        }
                    } finally {
                        LibUdev.INSTANCE.udev_device_unref(device);
                    }
                }

                Thread.currentThread().interrupt();
                errorLogger.logFailure("libudev device listening was interrupted");
                return Optional.empty();
            } catch (UnsatisfiedLinkError exception) {
                errorLogger.logFailure("Failed to load libudev native library", exception);
                return Optional.empty();
            } finally {
                if (monitor != null) {
                    LibUdev.INSTANCE.udev_monitor_unref(monitor);
                }
                if (udev != null) {
                    LibUdev.INSTANCE.udev_unref(udev);
                }
            }
        }

        private Optional<String> resolveRelevantDevice(Pointer device) {
            String action = nullToEmpty(LibUdev.INSTANCE.udev_device_get_action(device));
            if (!"add".equals(action) && !"bind".equals(action)) {
                return Optional.empty();
            }

            String devNode = firstNonBlank(
                    LibUdev.INSTANCE.udev_device_get_devnode(device),
                    LibUdev.INSTANCE.udev_device_get_property_value(device, "DEVNAME"));
            if (devNode == null) {
                return Optional.empty();
            }
            return Optional.of(devNode);
        }

        private void sleepQuietly() {
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        private boolean isLinux() {
            return System.getProperty("os.name", "").toLowerCase().contains("linux");
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        private String firstNonBlank(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first;
            }
            if (second != null && !second.isBlank()) {
                return second;
            }
            return null;
        }
    }
}
