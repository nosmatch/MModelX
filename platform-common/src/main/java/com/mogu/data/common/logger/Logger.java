package com.mogu.data.common.logger;

import java.util.logging.Level;

/**
 * Simple Logger Utility with formatting support
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
public class Logger {

    private final java.util.logging.Logger logger;

    private Logger(Class<?> clazz) {
        this.logger = java.util.logging.Logger.getLogger(clazz.getName());
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    public void info(String message) {
        logger.log(Level.INFO, message);
    }

    public void info(String format, Object... args) {
        logger.log(Level.INFO, formatMessage(format, args));
    }

    public void debug(String message) {
        logger.log(Level.FINE, message);
    }

    public void debug(String format, Object... args) {
        logger.log(Level.FINE, formatMessage(format, args));
    }

    public void warn(String message) {
        logger.log(Level.WARNING, message);
    }

    public void warn(String format, Object... args) {
        logger.log(Level.WARNING, formatMessage(format, args));
    }

    public void warn(String message, Throwable throwable) {
        logger.log(Level.WARNING, message, throwable);
    }

    public void error(String message) {
        logger.log(Level.SEVERE, message);
    }

    public void error(String format, Object... args) {
        logger.log(Level.SEVERE, formatMessage(format, args));
    }

    private String formatMessage(String format, Object... args) {
        String result = format;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", arg != null ? arg.toString() : "null");
        }
        return result;
    }

    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
