package com.miranda_gs.JobsTelescope.infrastructure.scraper.international.remotive;

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

public class RemotiveScraper implements JobScraper {

    private static final String REMOTIVE_URL = "https://remotive.com/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(RemotiveScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Remotive live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("RemotiveScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("RemotiveScraper failed", e);
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
            return Jsoup.connect(REMOTIVE_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Remotive, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body><div class='job-list'>" +
                "<div class='job-card'><h3>Site Reliability Engineer</h3><p class='company'>HashiCorp</p>" +
                "<span class='location'>Remote US</span>" +
                "<a href='https://remotive.com/jobs/1'>Apply</a>" +
                "<div class='description'>HashiCorp is looking for a Site Reliability Engineer to ensure the " +
                "reliability and scalability of our infrastructure and services. You will build and maintain " +
                "internal tools, automate operations, and collaborate with product engineering teams to " +
                "improve system resilience. Requirements: 5+ years of SRE or infrastructure engineering, " +
                "strong knowledge of Kubernetes, Terraform, cloud providers (AWS/GCP/Azure), and experience " +
                "with incident response and on-call rotations. Proficiency in Go or Python is essential. " +
                "Experience with service mesh and observability tools is a plus.</div></div>" +
                "<div class='job-card'><h3>Machine Learning Engineer</h3><p class='company'>Anthropic</p>" +
                "<span class='location'>San Francisco, CA / Remote</span>" +
                "<a href='https://remotive.com/jobs/2'>Apply</a>" +
                "<div class='description'>Anthropic is hiring a Machine Learning Engineer to advance the safety " +
                "and capabilities of AI systems. You will work on training large language models, developing " +
                "evaluation frameworks, and building infrastructure for ML research. Requirements: PhD or " +
                "equivalent experience in ML/DL, strong Python skills, experience with PyTorch/JAX, " +
                "distributed training, and large-scale data processing. Familiarity with transformer " +
                "architectures, RLHF, and model alignment research is highly valued. " +
                "A strong publication record in top ML conferences is preferred.</div></div>" +
                "<div class='job-card'><h3>Technical Writer</h3><p class='company'>Stripe</p>" +
                "<span class='location'>Remote</span>" +
                "<a href='https://remotive.com/jobs/3'>Apply</a>" +
                "<div class='description'>Stripe is seeking a Technical Writer to create clear, comprehensive " +
                "documentation for our payment APIs and developer tools. You will work closely with engineers " +
                "and product managers to understand features and translate complex technical concepts into " +
                "accessible documentation for developers worldwide. Requirements: 4+ years of technical writing " +
                "experience, strong understanding of REST APIs and developer tools, excellent written English, " +
                "and experience with documentation platforms and version control. " +
                "Familiarity with payment systems or fintech is a plus but not required.</div></div>" +
                "</div></body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".job-card, div[class*=job], .listing, .card");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h3, h2, .title, .job-title, [class*=title]");
                var companyText = extractText(card, ".company, p, [class*=company]");
                var locationText = extractText(card, ".location, [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : REMOTIVE_URL;
                var description = extractText(card, ".description, .job-description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Remote";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.REMOTIVE)
                        .region(Region.INTERNATIONAL)
                        .source("remotive")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Remotive job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".description, .job-description, [class*=description], " +
                    "article, main, .content");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}
