package com.miranda_gs.JobsTelescope.infrastructure.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;
import com.miranda_gs.JobsTelescope.domain.port.Exporter;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JsonExporter implements Exporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Path OUTPUT_BASE = Path.of("output");
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final InfrastructureLogger log = new InfrastructureLogger(JsonExporter.class);

    @Override
    public void export(SearchResult result, List<Job> jobs) {
        var dateDir = OUTPUT_BASE.resolve(LocalDate.now().format(DATE_FMT));
        try {
            Files.createDirectories(dateDir);
        } catch (IOException e) {
            log.error("Failed to create output directory: {}", e);
            return;
        }

        var filePath = dateDir.resolve("results.json");
        try {
            mapper.writeValue(filePath.toFile(), jobs);
            log.info("Exported JSON: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write JSON file: {}", e);
        }
    }
}