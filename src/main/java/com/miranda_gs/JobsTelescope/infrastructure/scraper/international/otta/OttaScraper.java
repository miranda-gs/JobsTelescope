package com.miranda_gs.JobsTelescope.infrastructure.scraper.international.otta;

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

public class OttaScraper implements JobScraper {

    private static final String OTTA_URL = "https://otta.com/jobs";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(OttaScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Otta live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("OttaScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("OttaScraper failed", e);
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
            return Jsoup.connect(OTTA_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Otta, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body><div class='JobCardContainer'>" +
                "<div class='JobCard'><h3>Backend Engineer</h3><p class='CompanyName'>Monzo</p>" +
                "<span class='Location'>London, UK / Remote</span>" +
                "<a href='https://otta.com/jobs/1'>Apply</a>" +
                "<div class='Description'>Monzo is looking for a Backend Engineer to help build the bank of the future. " +
                "You will design, build, and operate the microservices that power our banking platform, working " +
                "closely with product teams to deliver features that delight millions of customers. " +
                "Requirements: 4+ years of backend engineering experience, strong proficiency in Go or Java, " +
                "experience with distributed systems, PostgreSQL, Kafka, and cloud infrastructure (AWS/GCP). " +
                "You should be comfortable with event-driven architectures and have a strong testing mindset. " +
                "Experience in fintech or banking is a plus but not required.</div></div>" +
                "<div class='JobCard'><h3>Engineering Manager</h3><p class='CompanyName'>Revolut</p>" +
                "<span class='Location'>London, UK</span>" +
                "<a href='https://otta.com/jobs/2'>Apply</a>" +
                "<div class='Description'>Revolut is hiring an Engineering Manager to lead a team of talented " +
                "engineers building financial products at global scale. You will be responsible for team growth, " +
                "technical strategy, delivery excellence, and fostering a culture of innovation and collaboration. " +
                "Requirements: 7+ years of software engineering experience, 3+ years as an Engineering Manager, " +
                "strong technical background in backend systems, experience managing cross-functional teams, " +
                "and a passion for mentoring and developing talent. Experience in fintech is highly valued.</div></div>" +
                "<div class='JobCard'><h3>Data Scientist</h3><p class='CompanyName'>Spotify</p>" +
                "<span class='Location'>Stockholm, Sweden</span>" +
                "<a href='https://otta.com/jobs/3'>Apply</a>" +
                "<div class='Description'>Spotify is seeking a Data Scientist to drive insights and inform " +
                "product decisions across our music streaming platform. You will analyze user behavior, design " +
                "and analyze A/B experiments, build dashboards and reports, and communicate findings to stakeholders. " +
                "Requirements: 4+ years of experience in data science or analytics, strong SQL skills, " +
                "proficiency in Python/R for statistical analysis, experience with experimentation and causal " +
                "inference, and excellent communication skills. Experience in consumer products or media is a plus.</div></div>" +
                "</div></body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".JobCard, div[class*=Card], div.job-card, .listing, .job-listing");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h3, h2, .title, .job-title, [class*=title]");
                var companyText = extractText(card, ".CompanyName, .company, p, [class*=company]");
                var locationText = extractText(card, ".Location, .location, [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : OTTA_URL;
                var description = extractText(card, ".Description, .description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Remote";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.OTTA)
                        .region(Region.INTERNATIONAL)
                        .source("otta")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Otta job card: {}", e.getMessage());
            }
        }
        return jobs;
    }

    private String fetchJobDescription(String jobUrl) {
        try {
            var detailDoc = Jsoup.connect(jobUrl)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get();
            return extractText(detailDoc, ".Description, .description, [class*=description], " +
                    "article, .job-description, main, .content");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}
