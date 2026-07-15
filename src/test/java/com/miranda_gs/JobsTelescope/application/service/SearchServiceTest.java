package com.miranda_gs.JobsTelescope.application.service;

import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.infrastructure.normalizer.JobDeduplicator;
import com.miranda_gs.JobsTelescope.infrastructure.normalizer.JobNormalizer;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.BrazilScraperFactory;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.InternationalScraperFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class SearchServiceTest {

    @Test
    void shouldSearchBrazilAndReturnResult() {
        var service = new SearchService(
                new BrazilScraperFactory(),
                new InternationalScraperFactory(),
                new JobNormalizer(),
                new JobDeduplicator(0.9));

        var request = SearchRequest.builder()
                .query("Java")
                .region(Region.BRAZIL)
                .build();

        var progress = new ArrayList<String>();
        var result = service.search(request, progress::add);

        assertThat(result.getJobs()).isNotEmpty();
        assertThat(progress).isNotEmpty();
        assertThat(progress).contains("normalizing", "deduplicating");
    }

    @Test
    void shouldSearchInternationalAndReturnResult() {
        var service = new SearchService(
                new BrazilScraperFactory(),
                new InternationalScraperFactory(),
                new JobNormalizer(),
                new JobDeduplicator(0.9));

        var request = SearchRequest.builder()
                .query("Engineer")
                .region(Region.INTERNATIONAL)
                .build();

        var result = service.search(request, s -> {});

        assertThat(result.getJobs()).isNotEmpty();
        assertThat(result.getJobs()).allMatch(j -> j.getRegion() == Region.INTERNATIONAL);
    }
}