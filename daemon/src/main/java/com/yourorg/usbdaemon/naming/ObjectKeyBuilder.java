package com.yourorg.usbdaemon.naming;

import java.nio.file.Path;
import java.util.Objects;

public final class ObjectKeyBuilder {
    public String build(Path mountPath, Path pcapFile) {
        Objects.requireNonNull(mountPath, "mountPath");
        Objects.requireNonNull(pcapFile, "pcapFile");

        Path normalizedMountPath = mountPath.normalize();
        Path normalizedPcapFile = pcapFile.normalize();
        if (!normalizedPcapFile.startsWith(normalizedMountPath)) {
            throw new IllegalArgumentException("pcap file must be located under mount path: " + normalizedPcapFile);
        }

        Path relativePath = normalizedMountPath.relativize(normalizedPcapFile);
        if (relativePath.getNameCount() < 4) {
            throw new IllegalArgumentException("Insufficient path depth for object key: " + relativePath);
        }

        int fileIndex = relativePath.getNameCount() - 1;
        int vehicleDirectoryIndex = fileIndex - 1;
        int collectionDateIndex = fileIndex - 2;
        int vehicleTypeIndex = fileIndex - 3;

        String vehicleType = relativePath.getName(vehicleTypeIndex).toString();
        String collectionDate = relativePath.getName(collectionDateIndex).toString();
        String vehicleDirectory = relativePath.getName(vehicleDirectoryIndex).toString();
        String fileName = relativePath.getFileName().toString();

        validatePcapFileName(fileName);
        validateCollectionDate(collectionDate);

        String yyyy = slice(collectionDate, 0, 4, "collectionDate");
        String mm = slice(collectionDate, 4, 6, "collectionDate");
        String dd = slice(collectionDate, 6, 8, "collectionDate");
        String dateKey = buildDateKey(collectionDate);
        String vehicleNumber = normalizeVehicleNumber(extractVehicleNumber(vehicleType, vehicleDirectory));

        return String.format("%s/%s/%s/%s/%s_%s/%s",
                vehicleType,
                yyyy,
                mm,
                dd,
                dateKey,
                vehicleNumber,
                fileName);
    }

    private void validatePcapFileName(String fileName) {
        if (!fileName.endsWith(".pcap")) {
            throw new IllegalArgumentException("Only pcap files are supported: " + fileName);
        }
    }

    private void validateCollectionDate(String collectionDate) {
        if (collectionDate.length() != 8 || !collectionDate.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("collectionDate must be YYYYMMDD: " + collectionDate);
        }
    }

    private String buildDateKey(String collectionDate) {
        return slice(collectionDate, 2, 8, "collectionDate");
    }

    private String normalizeVehicleNumber(String vehicleNumber) {
        String normalized = vehicleNumber.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("vehicle number must not be blank");
        }
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            return "v" + normalized.substring(1);
        }
        return "v" + normalized;
    }

    private String extractVehicleNumber(String vehicleType, String vehicleDirectory) {
        String expectedPrefix = vehicleType + "-";
        if (vehicleDirectory.startsWith(expectedPrefix) && vehicleDirectory.length() > expectedPrefix.length()) {
            return vehicleDirectory.substring(expectedPrefix.length());
        }

        int separatorIndex = vehicleDirectory.indexOf('-');
        if (separatorIndex >= 0 && separatorIndex < vehicleDirectory.length() - 1) {
            return vehicleDirectory.substring(separatorIndex + 1);
        }

        return vehicleDirectory;
    }

    private String slice(String value, int start, int end, String fieldName) {
        if (value.length() < end) {
            throw new IllegalArgumentException(fieldName + " must be at least " + end + " characters: " + value);
        }
        return value.substring(start, end);
    }
}
