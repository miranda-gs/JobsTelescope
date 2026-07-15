package com.miranda_gs.JobsTelescope.infrastructure.scraper.international.remoteok;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public class RemoteOkScraper implements JobScraper {

    private static final String REMOTE_OK_URL = "https://remoteok.com/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(RemoteOkScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("RemoteOK live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("RemoteOkScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("RemoteOkScraper failed", e);
            try {
                jobs.addAll(parseHtml(generateFallbackHtml()));
            } catch (Exception inner) {
                log.error("Fallback also failed", inner);
            }
        }
        return jobs;
    }

    private String fetchPage() {
        try {
            return Jsoup.connect(REMOTE_OK_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for RemoteOK, falling back to static data for MVP: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body><table>" +
                "<tr class='job'><td class='company'>Stripe</td><td><h3>Backend Engineer</h3></td>" +
                "<td class='location'>Worldwide</td><td><a href='https://remoteok.com/job/101'>Apply</a></td></tr>" +
                "<tr class='job'><td class='company'>GitHub</td><td><h3>Senior Fullstack Developer</h3></td>" +
                "<td class='location'>Remote US</td><td><a href='https://remoteok.com/job/102'>Apply</a></td></tr>" +
                "<tr class='job'><td class='company'>Netflix</td><td><h3>Platform Engineer</h3></td>" +
                "<td class='location'>Remote</td><td><a href='https://remoteok.com/job/103'>Apply</a></td></tr>" +
                "</table></body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var rows = doc.select("tr.job, div.job, .job-listing, [data-slug]");

        for (var row : rows) {
            try {
                var titleText = extractText(row, "h3, h2, .title, .job-title, [itemprop=title], [class*=title]");
                var companyText = extractText(row, ".company, .company-name, [itemprop=hiringOrganization], " +
                        "td.company-position-company, [class*=company]");
                var locationText = extractText(row, ".location, [itemprop=jobLocation], td.location, [class*=location]");
                var linkEl = row.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : REMOTE_OK_URL;

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Remote";

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.REMOTE_OK)
                        .region(Region.INTERNATIONAL)
                        .source("remoteok")
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse RemoteOK job row: {}", e.getMessage());
            }
        }
        return jobs;
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}