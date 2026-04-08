package com.yourorg.usbdaemon.app;

import com.yourorg.usbdaemon.config.AppConfig;
import com.yourorg.usbdaemon.config.ConfigLoader;
import com.yourorg.usbdaemon.device.DeviceEventListener;
import com.yourorg.usbdaemon.device.MountPathResolver;
import com.yourorg.usbdaemon.logging.DaemonLogger;
import com.yourorg.usbdaemon.logging.ErrorLogger;
import com.yourorg.usbdaemon.naming.ObjectKeyBuilder;
import com.yourorg.usbdaemon.scan.PcapScanner;
import com.yourorg.usbdaemon.storage.IngestUploader;
import com.yourorg.usbdaemon.storage.MinioStorageClient;

public final class DaemonApplication {
    private DaemonApplication() {
    }

    public static void main(String[] args) {
        ConfigLoader configLoader = new ConfigLoader();
        AppConfig appConfig = configLoader.loadDefault();

        DaemonLogger daemonLogger = new DaemonLogger();
        ErrorLogger errorLogger = new ErrorLogger();
        daemonLogger.logStartup(appConfig.getLogPath());

        MountPathResolver mountPathResolver = new MountPathResolver(appConfig, errorLogger);
        DeviceEventListener deviceEventListener = new DeviceEventListener(mountPathResolver, daemonLogger);
        PcapScanner pcapScanner = new PcapScanner(appConfig, daemonLogger, errorLogger);
        ObjectKeyBuilder objectKeyBuilder = new ObjectKeyBuilder();
        MinioStorageClient minioStorageClient = new MinioStorageClient(appConfig);
        IngestUploader ingestUploader =
                new IngestUploader(appConfig, minioStorageClient, objectKeyBuilder, daemonLogger, errorLogger);

        DaemonRunner daemonRunner =
                new DaemonRunner(deviceEventListener, pcapScanner, ingestUploader, daemonLogger, errorLogger);
        daemonRunner.runOnce();
    }
}
