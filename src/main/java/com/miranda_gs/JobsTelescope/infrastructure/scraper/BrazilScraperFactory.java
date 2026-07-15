package com.miranda_gs.JobsTelescope.infrastructure.scraper;

import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.gupy.GupyScraper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BrazilScraperFactory {

    private final Map<Platform, JobScraper> scrapers = new HashMap<>();

    public BrazilScraperFactory() {
        scrapers.put(Platform.GUPY, new GupyScraper());
    }

    public Optional<JobScraper> getScraper(Platform platform) {
        return Optional.ofNullable(scrapers.get(platform));
    }

    public List<JobScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}