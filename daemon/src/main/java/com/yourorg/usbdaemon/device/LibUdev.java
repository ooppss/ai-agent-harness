package com.yourorg.usbdaemon.device;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

interface LibUdev extends Library {
    LibUdev INSTANCE = Native.load("udev", LibUdev.class);

    Pointer udev_new();

    Pointer udev_unref(Pointer udev);

    Pointer udev_monitor_new_from_netlink(Pointer udev, String name);

    int udev_monitor_filter_add_match_subsystem_devtype(Pointer udevMonitor, String subsystem, String devtype);

    int udev_monitor_enable_receiving(Pointer udevMonitor);

    Pointer udev_monitor_receive_device(Pointer udevMonitor);

    Pointer udev_monitor_unref(Pointer udevMonitor);

    String udev_device_get_action(Pointer udevDevice);

    String udev_device_get_devnode(Pointer udevDevice);

    String udev_device_get_property_value(Pointer udevDevice, String key);

    Pointer udev_device_unref(Pointer udevDevice);
}
