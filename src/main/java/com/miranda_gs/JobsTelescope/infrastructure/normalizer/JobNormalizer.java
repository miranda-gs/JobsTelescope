package com.miranda_gs.JobsTelescope.infrastructure.normalizer;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JobNormalizer {

    private static final Map<String, String> TITLE_NORMALIZATION = Map.ofEntries(
            Map.entry("jr", "Junior"),
            Map.entry("junior", "Junior"),
            Map.entry("pl", "Pleno"),
            Map.entry("pleno", "Pleno"),
            Map.entry("sr", "Senior"),
            Map.entry("senior", "Senior"),
            Map.entry("staff", "Staff"),
            Map.entry("principal", "Principal")
    );

    public List<Job> normalize(List<Job> jobs) {
        return jobs.stream()
                .map(this::normalizeJob)
                .toList();
    }

    private Job normalizeJob(Job job) {
        var normalizedTitle = normalizeTitle(job.getTitle().getValue());
        var normalizedLocation = normalizeLocation(job.getLocation().getValue());

        return Job.builder()
                .title(new JobTitle(normalizedTitle))
                .company(job.getCompany().trim())
                .location(new Location(normalizedLocation))
                .url(job.getUrl())
                .platform(job.getPlatform())
                .region(job.getRegion())
                .source(job.getSource())
                .foundAt(job.getFoundAt())
                .build();
    }

    private String normalizeTitle(String raw) {
        var normalized = raw.trim();
        for (var entry : TITLE_NORMALIZATION.entrySet()) {
            // Replace abbreviations like "Dev Java Jr" -> standard
            if (normalized.toLowerCase().endsWith(" " + entry.getKey())) {
                normalized = normalized.substring(0, normalized.length() - entry.getKey().length() - 1)
                        + " " + entry.getValue();
            }
        }
        // Standardize "Desenvolvedor" -> "Developer" for consistency in mixed scrapers
        normalized = normalized.replace("Desenvolvedor", "Developer")
                .replace("desenvolvedor", "Developer");
        return normalized.trim();
    }

    private String normalizeLocation(String raw) {
        if (raw == null || raw.isBlank()) return "";
        var loc = raw.trim();
        if (loc.equalsIgnoreCase("remote") || loc.equalsIgnoreCase("remoto")) return "Remote";
        if (loc.equalsIgnoreCase("worldwide")) return "Remote (Worldwide)";
        return loc.replace("Brasil", "Brazil");
    }
}