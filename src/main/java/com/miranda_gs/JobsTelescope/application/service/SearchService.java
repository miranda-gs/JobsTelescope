package com.miranda_gs.JobsTelescope.application.service;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;
import com.miranda_gs.JobsTelescope.infrastructure.normalizer.JobDeduplicator;
import com.miranda_gs.JobsTelescope.infrastructure.normalizer.JobNormalizer;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.BrazilScraperFactory;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.InternationalScraperFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SearchService {

    private final BrazilScraperFactory brazilFactory;
    private final InternationalScraperFactory internationalFactory;
    private final JobNormalizer normalizer;
    private final JobDeduplicator deduplicator;
    private final InfrastructureLogger log = new InfrastructureLogger(SearchService.class);

    public SearchService() {
        this.brazilFactory = new BrazilScraperFactory();
        this.internationalFactory = new InternationalScraperFactory();
        this.normalizer = new JobNormalizer();
        this.deduplicator = new JobDeduplicator(0.9);
    }

    public SearchService(
            BrazilScraperFactory brazilFactory,
            InternationalScraperFactory internationalFactory,
            JobNormalizer normalizer,
            JobDeduplicator deduplicator) {
        this.brazilFactory = brazilFactory;
        this.internationalFactory = internationalFactory;
        this.normalizer = normalizer;
        this.deduplicator = deduplicator;
    }

    public SearchResult search(SearchRequest request, Consumer<String> progressCallback) {
        var allJobs = new ArrayList<Job>();

        if (request.getRegion() == Region.BRAZIL) {
            allJobs.addAll(scrape(brazilFactory.getAllScrapers(), request, progressCallback));
        } else if (request.getRegion() == Region.INTERNATIONAL) {
            allJobs.addAll(scrape(internationalFactory.getAllScrapers(), request, progressCallback));
        } else {
            allJobs.addAll(scrape(brazilFactory.getAllScrapers(), request, progressCallback));
            allJobs.addAll(scrape(internationalFactory.getAllScrapers(), request, progressCallback));
        }

        int totalFound = allJobs.size();
        progressCallback.accept("normalizing");
        var normalized = normalizer.normalize(allJobs);
        progressCallback.accept("deduplicating");
        var deduplicated = deduplicator.deduplicate(normalized);
        int deduplicatedCount = totalFound - deduplicated.size();

        log.info("Search complete: {} found, {} after dedup", totalFound, deduplicated.size());

        return SearchResult.builder()
                .request(request)
                .jobs(deduplicated)
                .totalFound(totalFound)
                .totalDeduplicated(deduplicatedCount)
                .build();
    }

    private List<Job> scrape(List<JobScraper> scrapers, SearchRequest request,
                             Consumer<String> progressCallback) {
        var jobs = new ArrayList<Job>();
        for (var scraper : scrapers) {
            progressCallback.accept(scraper.getClass().getSimpleName());
            try {
                jobs.addAll(scraper.search(request));
            } catch (Exception e) {
                log.error("Scraper failed: " + scraper.getClass().getSimpleName(), e);
            }
        }
        return jobs;
    }
}