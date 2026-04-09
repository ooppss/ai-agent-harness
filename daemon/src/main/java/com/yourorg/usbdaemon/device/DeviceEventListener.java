package com.yourorg.usbdaemon.device;

import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.sun.jna.Pointer;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class DeviceEventListener {
    @FunctionalInterface
    public interface StoragePathAwaiter {
        Optional<Path> awaitNextStoragePath();
    }

    @FunctionalInterface
    interface UsbStorageDeviceAwaiter {
        Optional<String> awaitNextUsbStorageDevice();
    }

    @FunctionalInterface
    interface OsDetector {
        boolean isLinux();
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    interface LibUdevFacade {
        Pointer udevNew();

        Pointer udevUnref(Pointer udev);

        Pointer monitorNewFromNetlink(Pointer udev, String name);

        int monitorFilterAddMatchSubsystemDevtype(Pointer monitor, String subsystem, String devtype);

        int monitorEnableReceiving(Pointer monitor);

        Pointer monitorReceiveDevice(Pointer monitor);

        Pointer monitorUnref(Pointer monitor);

        String deviceGetAction(Pointer device);

        String deviceGetDevnode(Pointer device);

        String deviceGetPropertyValue(Pointer device, String key);

        Pointer deviceUnref(Pointer device);

        static LibUdevFacade nativeFacade() {
            return new LibUdevFacade() {
                @Override
                public Pointer udevNew() {
                    return LibUdev.INSTANCE.udev_new();
                }

                @Override
                public Pointer udevUnref(Pointer udev) {
                    return LibUdev.INSTANCE.udev_unref(udev);
                }

                @Override
                public Pointer monitorNewFromNetlink(Pointer udev, String name) {
                    return LibUdev.INSTANCE.udev_monitor_new_from_netlink(udev, name);
                }

                @Override
                public int monitorFilterAddMatchSubsystemDevtype(Pointer monitor, String subsystem, String devtype) {
                    return LibUdev.INSTANCE.udev_monitor_filter_add_match_subsystem_devtype(monitor, subsystem, devtype);
                }

                @Override
                public int monitorEnableReceiving(Pointer monitor) {
                    return LibUdev.INSTANCE.udev_monitor_enable_receiving(monitor);
                }

                @Override
                public Pointer monitorReceiveDevice(Pointer monitor) {
                    return LibUdev.INSTANCE.udev_monitor_receive_device(monitor);
                }

                @Override
                public Pointer monitorUnref(Pointer monitor) {
                    return LibUdev.INSTANCE.udev_monitor_unref(monitor);
                }

                @Override
                public String deviceGetAction(Pointer device) {
                    return LibUdev.INSTANCE.udev_device_get_action(device);
                }

                @Override
                public String deviceGetDevnode(Pointer device) {
                    return LibUdev.INSTANCE.udev_device_get_devnode(device);
                }

                @Override
                public String deviceGetPropertyValue(Pointer device, String key) {
                    return LibUdev.INSTANCE.udev_device_get_property_value(device, key);
                }

                @Override
                public Pointer deviceUnref(Pointer device) {
                    return LibUdev.INSTANCE.udev_device_unref(device);
                }
            };
        }
    }

    private final MountPathResolver mountPathResolver;
    private final DaemonLogger daemonLogger;
    private final ErrorLogger errorLogger;
    private final StoragePathAwaiter storagePathAwaiter;
    private final UsbStorageDeviceAwaiter usbStorageDeviceAwaiter;

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
            StoragePathAwaiter storagePathAwaiter) {
        this(mountPathResolver, daemonLogger, errorLogger, storagePathAwaiter, null);
    }

    DeviceEventListener(
            MountPathResolver mountPathResolver,
            DaemonLogger daemonLogger,
            ErrorLogger errorLogger,
            StoragePathAwaiter storagePathAwaiter,
            UsbStorageDeviceAwaiter usbStorageDeviceAwaiter) {
        this.mountPathResolver = mountPathResolver;
        this.daemonLogger = Objects.requireNonNull(daemonLogger, "daemonLogger");
        this.errorLogger = Objects.requireNonNull(errorLogger, "errorLogger");
        this.storagePathAwaiter = storagePathAwaiter;
        this.usbStorageDeviceAwaiter = usbStorageDeviceAwaiter != null
                ? usbStorageDeviceAwaiter
                : new LibUdevDeviceNameAwaiter(this.daemonLogger, this.errorLogger);
    }

    public Optional<Path> awaitNextStoragePath() {
        if (storagePathAwaiter != null) {
            return storagePathAwaiter.awaitNextStoragePath();
        }

        if (mountPathResolver == null) {
            errorLogger.logFailure("MountPathResolver is required for operational USB storage listening");
            return Optional.empty();
        }

        Optional<Path> storagePath = mountPathResolver.confirmConfiguredPathAvailable();
        if (storagePath.isPresent()) {
            daemonLogger.logStoragePathReady("configured-path", storagePath.get());
            return storagePath;
        }

        Optional<String> deviceName = usbStorageDeviceAwaiter.awaitNextUsbStorageDevice();
        if (deviceName.isEmpty()) {
            return Optional.empty();
        }

        storagePath = mountPathResolver.confirmStoragePathAvailable(deviceName.get());
        storagePath.ifPresent(path -> daemonLogger.logStoragePathReady(deviceName.get(), path));
        return storagePath;
    }

    static final class LibUdevDeviceNameAwaiter implements UsbStorageDeviceAwaiter {
        private static final long POLL_INTERVAL_MILLIS = 200L;

        private final DaemonLogger daemonLogger;
        private final ErrorLogger errorLogger;
        private final LibUdevFacade libUdevFacade;
        private final OsDetector osDetector;
        private final Sleeper sleeper;

        private LibUdevDeviceNameAwaiter(DaemonLogger daemonLogger, ErrorLogger errorLogger) {
            this(
                    daemonLogger,
                    errorLogger,
                    LibUdevFacade.nativeFacade(),
                    () -> System.getProperty("os.name", "").toLowerCase().contains("linux"),
                    Thread::sleep);
        }

        LibUdevDeviceNameAwaiter(
                DaemonLogger daemonLogger,
                ErrorLogger errorLogger,
                LibUdevFacade libUdevFacade,
                OsDetector osDetector,
                Sleeper sleeper) {
            this.daemonLogger = daemonLogger;
            this.errorLogger = errorLogger;
            this.libUdevFacade = Objects.requireNonNull(libUdevFacade, "libUdevFacade");
            this.osDetector = Objects.requireNonNull(osDetector, "osDetector");
            this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
        }

        @Override
        public Optional<String> awaitNextUsbStorageDevice() {
            if (!osDetector.isLinux()) {
                errorLogger.logFailure("libudev USB storage listening is only supported on Linux");
                return Optional.empty();
            }

            Pointer udev = null;
            Pointer monitor = null;
            try {
                udev = libUdevFacade.udevNew();
                if (udev == null) {
                    errorLogger.logFailure("Failed to initialize libudev context");
                    return Optional.empty();
                }

                monitor = libUdevFacade.monitorNewFromNetlink(udev, "udev");
                if (monitor == null) {
                    errorLogger.logFailure("Failed to create libudev monitor");
                    return Optional.empty();
                }

                if (libUdevFacade.monitorFilterAddMatchSubsystemDevtype(monitor, "block", null) != 0) {
                    errorLogger.logFailure("Failed to configure libudev block filter");
                    return Optional.empty();
                }

                if (libUdevFacade.monitorEnableReceiving(monitor) != 0) {
                    errorLogger.logFailure("Failed to enable libudev monitor receiving");
                    return Optional.empty();
                }

                while (!Thread.currentThread().isInterrupted()) {
                    Pointer device = libUdevFacade.monitorReceiveDevice(monitor);
                    if (device == null) {
                        sleepQuietly();
                        continue;
                    }

                    try {
                        Optional<String> deviceName = resolveRelevantUsbStorageDevice(device);
                        if (deviceName.isPresent()) {
                            daemonLogger.logUsbStorageDetected(deviceName.get());
                            return deviceName;
                        }
                    } finally {
                        libUdevFacade.deviceUnref(device);
                    }
                }

                Thread.currentThread().interrupt();
                errorLogger.logFailure("libudev USB storage listening was interrupted");
                return Optional.empty();
            } catch (UnsatisfiedLinkError exception) {
                errorLogger.logFailure("Failed to load libudev native library", exception);
                return Optional.empty();
            } finally {
                if (monitor != null) {
                    libUdevFacade.monitorUnref(monitor);
                }
                if (udev != null) {
                    libUdevFacade.udevUnref(udev);
                }
            }
        }

        private Optional<String> resolveRelevantUsbStorageDevice(Pointer device) {
            String action = nullToEmpty(libUdevFacade.deviceGetAction(device));
            if (!"add".equals(action) && !"bind".equals(action)) {
                return Optional.empty();
            }

            String bus = nullToEmpty(libUdevFacade.deviceGetPropertyValue(device, "ID_BUS"));
            if (!"usb".equalsIgnoreCase(bus)) {
                return Optional.empty();
            }

            String devType = nullToEmpty(libUdevFacade.deviceGetPropertyValue(device, "DEVTYPE"));
            if (!devType.isBlank()
                    && !"disk".equalsIgnoreCase(devType)
                    && !"partition".equalsIgnoreCase(devType)) {
                return Optional.empty();
            }

            String devNode = firstNonBlank(
                    libUdevFacade.deviceGetDevnode(device),
                    libUdevFacade.deviceGetPropertyValue(device, "DEVNAME"));
            if (devNode == null) {
                return Optional.empty();
            }
            return Optional.of(devNode);
        }

        private void sleepQuietly() {
            try {
                sleeper.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
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
