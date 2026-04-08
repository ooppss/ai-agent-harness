package com.yourorg.usbdaemon.naming;

import java.nio.file.Path;

public final class ObjectKeyBuilder {
    public String build(Path mountPath, Path pcapFile) {
        Path relativePath = mountPath.relativize(pcapFile);
        if (relativePath.getNameCount() < 4) {
            throw new IllegalArgumentException("Insufficient path depth for object key: " + relativePath);
        }

        String vehicleType = relativePath.getName(0).toString();
        String collectionDate = relativePath.getName(1).toString();
        String vehicleId = relativePath.getName(2).toString();
        String fileName = relativePath.getFileName().toString();

        String yyyy = slice(collectionDate, 0, 4, "collectionDate");
        String mm = slice(collectionDate, 4, 6, "collectionDate");
        String dd = slice(collectionDate, 6, 8, "collectionDate");
        String dateKey = slice(collectionDate, 2, 8, "collectionDate");
        String normalizedVehicleId = vehicleId.replace('-', '_').toLowerCase();

        return String.format("%s/%s/%s/%s/%s_%s/%s",
                vehicleType,
                yyyy,
                mm,
                dd,
                dateKey,
                normalizedVehicleId,
                fileName);
    }

    private String slice(String value, int start, int end, String fieldName) {
        if (value.length() < end) {
            throw new IllegalArgumentException(fieldName + " must be at least " + end + " characters: " + value);
        }
        return value.substring(start, end);
    }
}
