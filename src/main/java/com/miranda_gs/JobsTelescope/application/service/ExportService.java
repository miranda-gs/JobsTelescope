package com.miranda_gs.JobsTelescope.application.service;

import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;
import com.miranda_gs.JobsTelescope.domain.port.Exporter;
import com.miranda_gs.JobsTelescope.infrastructure.exporter.MarkdownExporter;
import com.miranda_gs.JobsTelescope.infrastructure.exporter.JsonExporter;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;

import java.util.List;

public class ExportService {

    private final List<Exporter> exporters;
    private final InfrastructureLogger log = new InfrastructureLogger(ExportService.class);

    public ExportService() {
        this.exporters = List.of(new MarkdownExporter(), new JsonExporter());
    }

    public ExportService(List<Exporter> exporters) {
        this.exporters = exporters;
    }

    public void export(SearchResult result) {
        var jobs = result.getJobs();
        for (var exporter : exporters) {
            try {
                exporter.export(result, jobs);
                log.info("Exported with {}", exporter.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Export failed for: " + exporter.getClass().getSimpleName(), e);
            }
        }
    }
}