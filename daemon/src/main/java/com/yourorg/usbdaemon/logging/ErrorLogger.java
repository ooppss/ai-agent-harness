package com.yourorg.usbdaemon.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ErrorLogger {
    private final Logger logger = Logger.getLogger(ErrorLogger.class.getName());

    public void logFailure(String message) {
        logger.severe(message);
    }

    public void logFailure(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
