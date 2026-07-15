package com.miranda_gs.JobsTelescope.infrastructure.normalizer;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobDeduplicatorTest {

    private final JobDeduplicator deduplicator = new JobDeduplicator(0.9);

    @Test
    void shouldRemoveExactDuplicates() {
        var job1 = buildJob("Java Developer", "Google", "https://a.com");
        var job2 = buildJob("Java Developer", "Google", "https://b.com");

        var result = deduplicator.deduplicate(List.of(job1, job2));
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldKeepDifferentJobs() {
        var job1 = buildJob("Java Developer", "Google", "https://a.com");
        var job2 = buildJob("Python Developer", "Meta", "https://b.com");

        var result = deduplicator.deduplicate(List.of(job1, job2));
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldHandleEmptyList() {
        var result = deduplicator.deduplicate(List.of());
        assertThat(result).isEmpty();
    }

    private Job buildJob(String title, String company, String url) {
        return Job.builder()
                .title(new JobTitle(title))
                .company(company)
                .url(new Url(url))
                .platform(Platform.GUPY)
                .region(Region.BRAZIL)
                .location(new Location("Remote"))
                .source("test")
                .build();
    }
}