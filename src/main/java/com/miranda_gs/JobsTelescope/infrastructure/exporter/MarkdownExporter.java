package com.miranda_gs.JobsTelescope.infrastructure.exporter;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;
import com.miranda_gs.JobsTelescope.domain.port.Exporter;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MarkdownExporter implements Exporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Path OUTPUT_BASE = Path.of("output");
    private final InfrastructureLogger log = new InfrastructureLogger(MarkdownExporter.class);

    @Override
    public void export(SearchResult result, List<Job> jobs) {
        var dateDir = OUTPUT_BASE.resolve(LocalDate.now().format(DATE_FMT));
        try {
            Files.createDirectories(dateDir);
        } catch (IOException e) {
            log.error("Failed to create output directory: {}", e);
            return;
        }

        for (int i = 0; i < jobs.size(); i++) {
            var job = jobs.get(i);
            var filename = sanitizeFilename(job.getTitle().getValue(), job.getCompany(), i);
            var filePath = dateDir.resolve(filename);

            try {
                Files.writeString(filePath, buildMarkdownContent(job));
                log.info("Exported: {}", filePath);
            } catch (IOException e) {
                log.error("Failed to write markdown file: {}", e);
            }
        }
    }

    private String buildMarkdownContent(Job job) {
        return new StringBuilder()
                .append("# ").append(job.getTitle().getValue()).append("\n\n")
                .append("**Empresa:** ").append(job.getCompany()).append("\n\n")
                .append("**Localização:** ").append(job.getLocation().getValue()).append("\n\n")
                .append("**Plataforma:** ").append(job.getPlatform().name()).append("\n\n")
                .append("**Região:** ").append(job.getRegion().name()).append("\n\n")
                .append("**Fonte:** ").append(job.getSource()).append("\n\n")
                .append("**Link:** [Ver vaga](").append(job.getUrl().getValue()).append(")\n\n")
                .append("**Data de coleta:** ").append(job.getFoundAt().format(DATE_FMT)).append("\n")
                .toString();
    }

    private String sanitizeFilename(String title, String company, int index) {
        var raw = (title + "-" + company)
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (raw.length() > 100) raw = raw.substring(0, 100);
        return raw + ".md";
    }
}