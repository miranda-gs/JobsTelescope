package com.miranda_gs.JobsTelescope.domain.valueobject;

import lombok.Value;

@Value
public class Location {
    String value;

    public Location(String value) {
        this.value = value != null ? value.trim() : "";
    }
}