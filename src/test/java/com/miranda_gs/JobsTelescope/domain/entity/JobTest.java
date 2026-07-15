package com.miranda_gs.JobsTelescope.domain.entity;

import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobTest {

    @Test
    void shouldCreateJobWithAllFields() {
        var job = Job.builder()
                .title(new JobTitle("Java Backend Developer"))
                .company("Google")
                .location(new Location("Remote"))
                .url(new Url("https://example.com/job/1"))
                .platform(Platform.GUPY)
                .region(Region.BRAZIL)
                .source("gupy")
                .build();

        assertThat(job.getTitle().getValue()).isEqualTo("Java Backend Developer");
        assertThat(job.getCompany()).isEqualTo("Google");
        assertThat(job.getLocation().getValue()).isEqualTo("Remote");
        assertThat(job.getUrl().getValue()).isEqualTo("https://example.com/job/1");
        assertThat(job.getPlatform()).isEqualTo(Platform.GUPY);
        assertThat(job.getRegion()).isEqualTo(Region.BRAZIL);
        assertThat(job.getSource()).isEqualTo("gupy");
        assertThat(job.getFoundAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldDefaultFoundAtToToday() {
        var job = Job.builder()
                .title(new JobTitle("Dev"))
                .company("ACME")
                .url(new Url("https://a.com"))
                .platform(Platform.REMOTE_OK)
                .region(Region.INTERNATIONAL)
                .location(new Location("Anywhere"))
                .source("remoteok")
                .build();

        assertThat(job.getFoundAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldRejectInvalidTitle() {
        assertThatThrownBy(() -> new JobTitle(" "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new JobTitle(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidUrl() {
        assertThatThrownBy(() -> new Url(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}