package com.yourorg.usbdaemon.device;

import com.sun.jna.Pointer;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibUdevDeviceNameAwaiterTest {
    @Test
    void returnsEmptyWithoutCallingLibUdevWhenOsIsNotLinux() {
        FakeLibUdevFacade libUdevFacade = new FakeLibUdevFacade();
        DeviceEventListener.LibUdevDeviceNameAwaiter awaiter = new DeviceEventListener.LibUdevDeviceNameAwaiter(
                new DaemonLogger(),
                new ErrorLogger(),
                libUdevFacade,
                () -> false,
                millis -> {
                });

        Optional<String> deviceName = awaiter.awaitNextUsbStorageDevice();

        assertFalse(deviceName.isPresent());
        assertEquals(0, libUdevFacade.udevNewCount);
    }

    @Test
    void waitsForRelevantAddEventAndReturnsDevnode() {
        FakeLibUdevFacade libUdevFacade = new FakeLibUdevFacade();
        Pointer ignoredDevice = libUdevFacade.enqueueDevice("change", "/dev/sda1", null, "usb", "disk");
        Pointer expectedDevice = libUdevFacade.enqueueDevice("add", "/dev/sdb1", null, "usb", "disk");
        AtomicInteger sleepCount = new AtomicInteger();

        DeviceEventListener.LibUdevDeviceNameAwaiter awaiter = new DeviceEventListener.LibUdevDeviceNameAwaiter(
                new DaemonLogger(),
                new ErrorLogger(),
                libUdevFacade,
                () -> true,
                millis -> sleepCount.incrementAndGet());

        Optional<String> deviceName = awaiter.awaitNextUsbStorageDevice();

        assertTrue(deviceName.isPresent());
        assertEquals("/dev/sdb1", deviceName.get());
        assertEquals(1, sleepCount.get());
        assertEquals(2, libUdevFacade.deviceUnrefCount);
        assertTrue(libUdevFacade.unrefedDevices.containsKey(ignoredDevice));
        assertTrue(libUdevFacade.unrefedDevices.containsKey(expectedDevice));
        assertEquals(1, libUdevFacade.monitorUnrefCount);
        assertEquals(1, libUdevFacade.udevUnrefCount);
    }

    @Test
    void usesDevnamePropertyForBindEventWhenDevnodeIsMissing() {
        FakeLibUdevFacade libUdevFacade = new FakeLibUdevFacade();
        libUdevFacade.enqueueDevice("bind", null, "/dev/sdc1", "usb", "partition");

        DeviceEventListener.LibUdevDeviceNameAwaiter awaiter = new DeviceEventListener.LibUdevDeviceNameAwaiter(
                new DaemonLogger(),
                new ErrorLogger(),
                libUdevFacade,
                () -> true,
                millis -> {
                });

        Optional<String> deviceName = awaiter.awaitNextUsbStorageDevice();

        assertTrue(deviceName.isPresent());
        assertEquals("/dev/sdc1", deviceName.get());
    }

    @Test
    void ignoresNonUsbBlockDeviceEvents() {
        FakeLibUdevFacade libUdevFacade = new FakeLibUdevFacade();
        libUdevFacade.enqueueDevice("add", "/dev/sdb1", null, "ata", "disk");

        DeviceEventListener.LibUdevDeviceNameAwaiter awaiter = new DeviceEventListener.LibUdevDeviceNameAwaiter(
                new DaemonLogger(),
                new ErrorLogger(),
                libUdevFacade,
                () -> true,
                millis -> Thread.currentThread().interrupt());

        Optional<String> deviceName = awaiter.awaitNextUsbStorageDevice();

        assertFalse(deviceName.isPresent());
    }

    private static final class FakeLibUdevFacade implements DeviceEventListener.LibUdevFacade {
        private final Pointer udev = new Pointer(1);
        private final Pointer monitor = new Pointer(2);
        private final Queue<Pointer> receiveQueue = new ArrayDeque<>();
        private final Map<Pointer, String> actions = new HashMap<>();
        private final Map<Pointer, String> devnodes = new HashMap<>();
        private final Map<Pointer, String> devnames = new HashMap<>();
        private final Map<Pointer, String> buses = new HashMap<>();
        private final Map<Pointer, String> devtypes = new HashMap<>();
        private final Map<Pointer, Boolean> unrefedDevices = new HashMap<>();
        private int nullPollsRemaining = 1;
        private int udevNewCount;
        private int udevUnrefCount;
        private int monitorUnrefCount;
        private int deviceUnrefCount;
        private long pointerValue = 10;

        Pointer enqueueDevice(String action, String devnode, String devname, String bus, String devtype) {
            Pointer device = new Pointer(pointerValue++);
            actions.put(device, action);
            devnodes.put(device, devnode);
            devnames.put(device, devname);
            buses.put(device, bus);
            devtypes.put(device, devtype);
            receiveQueue.add(device);
            return device;
        }

        @Override
        public Pointer udevNew() {
            udevNewCount++;
            return udev;
        }

        @Override
        public Pointer udevUnref(Pointer udev) {
            udevUnrefCount++;
            return udev;
        }

        @Override
        public Pointer monitorNewFromNetlink(Pointer udev, String name) {
            return monitor;
        }

        @Override
        public int monitorFilterAddMatchSubsystemDevtype(Pointer monitor, String subsystem, String devtype) {
            return 0;
        }

        @Override
        public int monitorEnableReceiving(Pointer monitor) {
            return 0;
        }

        @Override
        public Pointer monitorReceiveDevice(Pointer monitor) {
            if (nullPollsRemaining > 0) {
                nullPollsRemaining--;
                return null;
            }
            return receiveQueue.isEmpty() ? null : receiveQueue.remove();
        }

        @Override
        public Pointer monitorUnref(Pointer monitor) {
            monitorUnrefCount++;
            return monitor;
        }

        @Override
        public String deviceGetAction(Pointer device) {
            return actions.get(device);
        }

        @Override
        public String deviceGetDevnode(Pointer device) {
            return devnodes.get(device);
        }

        @Override
        public String deviceGetPropertyValue(Pointer device, String key) {
            if ("DEVNAME".equals(key)) {
                return devnames.get(device);
            }
            if ("ID_BUS".equals(key)) {
                return buses.get(device);
            }
            if ("DEVTYPE".equals(key)) {
                return devtypes.get(device);
            }
            return null;
        }

        @Override
        public Pointer deviceUnref(Pointer device) {
            deviceUnrefCount++;
            unrefedDevices.put(device, Boolean.TRUE);
            return device;
        }
    }
}
