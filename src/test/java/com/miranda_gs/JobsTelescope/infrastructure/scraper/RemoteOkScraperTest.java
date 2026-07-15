package com.miranda_gs.JobsTelescope.infrastructure.scraper;

import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.remoteok.RemoteOkScraper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteOkScraperTest {

    @Test
    void shouldReturnJobsFromFallbackHtml() {
        var scraper = new RemoteOkScraper();
        var request = SearchRequest.builder()
                .query("Engineer")
                .region(Region.INTERNATIONAL)
                .build();

        var jobs = scraper.search(request);

        assertThat(jobs).isNotEmpty();
        assertThat(jobs).allMatch(j -> j.getRegion() == Region.INTERNATIONAL);
        assertThat(jobs).allMatch(j -> !j.getTitle().getValue().isBlank());
        assertThat(jobs).allMatch(j -> !j.getCompany().isBlank());
    }
}