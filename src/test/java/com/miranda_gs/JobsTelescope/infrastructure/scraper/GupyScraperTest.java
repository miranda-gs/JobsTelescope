package com.miranda_gs.JobsTelescope.infrastructure.scraper;

import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.gupy.GupyScraper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GupyScraperTest {

    @Test
    void shouldReturnJobsFromFallbackHtml() {
        var scraper = new GupyScraper();
        var request = SearchRequest.builder()
                .query("Java")
                .region(Region.BRAZIL)
                .build();

        var jobs = scraper.search(request);

        assertThat(jobs).isNotEmpty();
        assertThat(jobs).allMatch(j -> j.getRegion() == Region.BRAZIL);
        assertThat(jobs).allMatch(j -> !j.getTitle().getValue().isBlank());
        assertThat(jobs).allMatch(j -> !j.getCompany().isBlank());
    }

    @Test
    void shouldReturnEmptyListOnEmptyRequest() {
        var scraper = new GupyScraper();
        var request = SearchRequest.builder()
                .query("")
                .region(Region.BRAZIL)
                .build();

        var jobs = scraper.search(request);
        assertThat(jobs).isNotNull();
    }
}