package com.miranda_gs.JobsTelescope.domain.valueobject;

import lombok.Value;

@Value
public class Url {
    String value;

    public Url(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }
        this.value = value.trim();
    }
}