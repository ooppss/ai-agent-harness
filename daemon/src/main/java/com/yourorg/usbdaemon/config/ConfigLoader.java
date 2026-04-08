package com.yourorg.usbdaemon.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class ConfigLoader {
    private static final String DEFAULT_RESOURCE = "application.properties";

    public AppConfig loadDefault() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing default config resource: " + DEFAULT_RESOURCE);
            }
            return fromProperties(loadProperties(inputStream));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load default config resource", exception);
        }
    }

    public AppConfig load(Path propertiesFile) {
        try (InputStream inputStream = Files.newInputStream(propertiesFile)) {
            return fromProperties(loadProperties(inputStream));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load config file: " + propertiesFile, exception);
        }
    }

    private AppConfig fromProperties(Properties properties) {
        return new AppConfig(
                require(properties, "minio.endpoint"),
                require(properties, "minio.access-key"),
                require(properties, "minio.secret-key"),
                require(properties, "minio.bucket"),
                Paths.get(require(properties, "device.mount-path")),
                properties.getProperty("scan.relative-path", ""),
                Integer.parseInt(require(properties, "mount.retry-count")),
                Long.parseLong(require(properties, "mount.retry-interval-millis")),
                Paths.get(require(properties, "logging.path"))
        );
    }

    private Properties loadProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    private String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config key: " + key);
        }
        return value.trim();
    }
}
