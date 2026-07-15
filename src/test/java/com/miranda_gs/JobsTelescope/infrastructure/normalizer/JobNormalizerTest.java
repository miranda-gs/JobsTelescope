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

class JobNormalizerTest {

    private final JobNormalizer normalizer = new JobNormalizer();

    @Test
    void shouldNormalizeJuniorAbbreviation() {
        var job = Job.builder()
                .title(new JobTitle("Desenvolvedor Java Jr"))
                .company("ACME")
                .url(new Url("https://a.com"))
                .platform(Platform.GUPY)
                .region(Region.BRAZIL)
                .location(new Location("São Paulo"))
                .source("gupy")
                .build();

        var normalized = normalizer.normalize(List.of(job));
        assertThat(normalized.get(0).getTitle().getValue()).isEqualTo("Developer Java Junior");
    }

    @Test
    void shouldNormalizeLocationNames() {
        var job = buildJob("Developer", "remote", "Telecorp");
        var normalized = normalizer.normalize(List.of(job));
        assertThat(normalized.get(0).getLocation().getValue()).isEqualTo("Remote");
    }

    @Test
    void shouldNormalizeBrasilToBrazil() {
        var job = buildJob("Engineer", "Brasil", "DataCorp");
        var normalized = normalizer.normalize(List.of(job));
        assertThat(normalized.get(0).getLocation().getValue()).isEqualTo("Brazil");
    }

    @Test
    void shouldPreserveValidTitle() {
        var job = buildJob("Backend Engineer", "Remote US", "Stripe");
        var normalized = normalizer.normalize(List.of(job));
        assertThat(normalized.get(0).getTitle().getValue()).isEqualTo("Backend Engineer");
    }

    private Job buildJob(String title, String location, String company) {
        return Job.builder()
                .title(new JobTitle(title))
                .company(company)
                .url(new Url("https://example.com/j"))
                .platform(Platform.REMOTE_OK)
                .region(Region.INTERNATIONAL)
                .location(new Location(location))
                .source("test")
                .build();
    }
}