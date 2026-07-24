package com.miranda_gs.JobsTelescope.infrastructure.scraper;

import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.jobicy.JobicyScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.otta.OttaScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.remoteok.RemoteOkScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.remotive.RemotiveScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.wellfound.WellfoundScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.international.weworkremotely.WeWorkRemotelyScraper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InternationalScraperFactory {

    private final Map<Platform, JobScraper> scrapers = new HashMap<>();

    public InternationalScraperFactory() {
        scrapers.put(Platform.REMOTE_OK, new RemoteOkScraper());
        scrapers.put(Platform.WELLFOUND, new WellfoundScraper());
        scrapers.put(Platform.OTTA, new OttaScraper());
        scrapers.put(Platform.WE_WORK_REMOTELY, new WeWorkRemotelyScraper());
        scrapers.put(Platform.JOBICY, new JobicyScraper());
        scrapers.put(Platform.REMOTIVE, new RemotiveScraper());
    }

    public Optional<JobScraper> getScraper(Platform platform) {
        return Optional.ofNullable(scrapers.get(platform));
    }

    public List<JobScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}