package com.miranda_gs.JobsTelescope.infrastructure.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfrastructureLogger {

    private final Logger logger;

    public InfrastructureLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }
}