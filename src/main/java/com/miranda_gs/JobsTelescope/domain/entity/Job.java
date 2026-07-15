package com.miranda_gs.JobsTelescope.domain.entity;

import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Job {
    JobTitle title;
    String company;
    Location location;
    Url url;
    Platform platform;
    Region region;
    String source;
    @Builder.Default
    LocalDate foundAt = LocalDate.now();
}