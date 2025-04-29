package org.example;

import org.slf4j.LoggerFactory;

public final class Logger {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);
    
    private Logger() {
        // Private constructor to prevent instantiation
    }
    
    public static void debug(String message) {
        logger.debug(message);
    }
    
    public static void debug(String format, Object... args) {
        logger.debug(format, args);
    }
    
    public static void info(String message) {
        logger.info(message);
    }
    
    public static void info(String format, Object... args) {
        logger.info(format, args);
    }
    
    public static void warn(String message) {
        logger.warn(message);
    }
    
    public static void warn(String format, Object... args) {
        logger.warn(format, args);
    }
    
    public static void error(String message) {
        logger.error(message);
    }
    
    public static void error(String format, Object... args) {
        logger.error(format, args);
    }
    
    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
} 