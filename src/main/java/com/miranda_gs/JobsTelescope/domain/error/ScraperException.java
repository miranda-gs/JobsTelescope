package com.miranda_gs.JobsTelescope.domain.error;

public class ScraperException extends DomainException {

    public ScraperException(String message) {
        super(message);
    }

    public ScraperException(String message, Throwable cause) {
        super(message, cause);
    }
}