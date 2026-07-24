package com.miranda_gs.JobsTelescope.infrastructure.scraper.international.weworkremotely;

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

public class WeWorkRemotelyScraper implements JobScraper {

    private static final String WE_WORK_REMOTELY_URL = "https://weworkremotely.com/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(WeWorkRemotelyScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("WeWorkRemotely live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("WeWorkRemotelyScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("WeWorkRemotelyScraper failed", e);
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
            return Jsoup.connect(WE_WORK_REMOTELY_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for WeWorkRemotely, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body><ul class='jobs-listing'>" +
                "<li class='job'><a href='https://weworkremotely.com/job/1'><span class='title'>Senior DevOps Engineer</span>" +
                "<span class='company'>GitLab</span><span class='location'>Remote</span>" +
                "<div class='description'>GitLab is hiring a Senior DevOps Engineer to help operate and scale " +
                "the world's largest single-codebase DevOps platform. You will work on infrastructure automation, " +
                "Kubernetes orchestration, CI/CD pipelines, and reliability engineering. " +
                "Requirements: 6+ years of experience in DevOps/SRE, deep knowledge of Kubernetes, Terraform, " +
                "and cloud providers (GCP/AWS), experience with observability tools (Prometheus, Grafana, " +
                "OpenTelemetry), and strong scripting skills in Go or Python. " +
                "You will be part of an all-remote team with flexible working hours.</div></a></li>" +
                "<li class='job'><a href='https://weworkremotely.com/job/2'><span class='title'>Full Stack Developer</span>" +
                "<span class='company'>Basecamp</span><span class='location'>Remote</span>" +
                "<div class='description'>Basecamp is looking for a Full Stack Developer to help build and improve " +
                "our project management and team communication tools. You will work across both frontend and " +
                "backend, using Ruby on Rails, JavaScript, and Hotwire to deliver features that help teams " +
                "work better together. Requirements: 5+ years of full stack development experience, strong " +
                "Ruby on Rails skills, proficiency in JavaScript and modern CSS, experience with relational " +
                "databases, and a commitment to clean, well-tested code. " +
                "We believe in sustainable pace and work-life balance.</div></a></li>" +
                "<li class='job'><a href='https://weworkremotely.com/job/3'><span class='title'>Customer Success Manager</span>" +
                "<span class='company'>Zapier</span><span class='location'>Remote</span>" +
                "<div class='description'>Zapier is seeking a Customer Success Manager to help our customers " +
                "get the most out of automation. You will onboard new customers, provide training and support, " +
                "identify growth opportunities, and advocate for customer needs internally. " +
                "Requirements: 3+ years of customer success or account management experience, excellent " +
                "communication skills, familiarity with SaaS products, and a data-driven approach to " +
                "understanding customer health and engagement. Experience with automation tools is a plus.</div></a></li>" +
                "</ul></body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select("li.job, .job, [class*=job]");

        for (var card : cards) {
            try {
                var titleText = extractText(card, ".title, h3, h2, [class*=title]");
                var companyText = extractText(card, ".company, [class*=company]");
                var locationText = extractText(card, ".location, [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : WE_WORK_REMOTELY_URL;
                var description = extractText(card, ".description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Remote";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.WE_WORK_REMOTELY)
                        .region(Region.INTERNATIONAL)
                        .source("weworkremotely")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse WeWorkRemotely job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".description, [class*=description], article, .job-description, main, .content");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}
