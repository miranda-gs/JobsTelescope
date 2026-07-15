package com.miranda_gs.JobsTelescope.domain.error;

public class InvalidSearchRequestException extends DomainException {

    public InvalidSearchRequestException(String message) {
        super(message);
    }
}