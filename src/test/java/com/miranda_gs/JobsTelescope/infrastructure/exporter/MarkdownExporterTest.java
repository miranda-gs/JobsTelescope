package com.miranda_gs.JobsTelescope.infrastructure.exporter;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownExporterTest {

    private final MarkdownExporter exporter = new MarkdownExporter();

    @AfterEach
    void cleanup() throws Exception {
        var outputDir = Path.of("output");
        if (Files.exists(outputDir)) {
            try (var walk = Files.walk(outputDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
            }
        }
    }

    @Test
    void shouldWriteMarkdownFiles() throws Exception {
        var job = Job.builder()
                .title(new JobTitle("Java Developer"))
                .company("Google")
                .location(new Location("Remote"))
                .url(new Url("https://careers.google.com"))
                .platform(Platform.GUPY)
                .region(Region.BRAZIL)
                .source("gupy")
                .build();

        var result = SearchResult.builder()
                .request(SearchRequest.builder().query("Java").region(Region.BRAZIL).build())
                .jobs(java.util.List.of(job))
                .totalFound(1)
                .build();

        exporter.export(result, java.util.List.of(job));

        var date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        var outputDir = Path.of("output", date);
        assertThat(Files.exists(outputDir)).isTrue();

        var files = Files.list(outputDir).toList();
        assertThat(files).isNotEmpty();
    }
}