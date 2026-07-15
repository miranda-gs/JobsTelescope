package com.miranda_gs.JobsTelescope.domain.valueobject;

import lombok.Value;

@Value
public class JobTitle {
    String value;

    public JobTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Job title cannot be null or blank");
        }
        this.value = value.trim();
    }
}