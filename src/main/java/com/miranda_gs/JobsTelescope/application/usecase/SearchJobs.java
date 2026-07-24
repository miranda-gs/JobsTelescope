package com.miranda_gs.JobsTelescope.application.usecase;

import com.miranda_gs.JobsTelescope.application.service.ExportService;
import com.miranda_gs.JobsTelescope.application.service.SearchService;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;
import com.miranda_gs.JobsTelescope.infrastructure.io.JsonEventWriter;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SearchJobs {

    private final SearchService searchService;
    private final ExportService exportService;
    private final InfrastructureLogger log = new InfrastructureLogger(SearchJobs.class);

    public SearchJobs() {
        this.searchService = new SearchService();
        this.exportService = new ExportService();
    }

    public SearchJobs(SearchService searchService, ExportService exportService) {
        this.searchService = searchService;
        this.exportService = exportService;
    }

    public void execute(SearchRequest request, JsonEventWriter eventWriter) {
        log.info("SearchJobs: query={}, region={}", request.getQuery(), request.getRegion());

        var result = searchService.search(request, step ->
                eventWriter.writeProgress(step, estimateProgress(step)));

        exportService.export(result);

        var date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        var outputPath = "output/" + date;

        eventWriter.writeCompleted(result.getJobs().size(), outputPath);
        log.info("SearchJobs completed: {} jobs", result.getJobs().size());
    }

    private int estimateProgress(String step) {
        return switch (step) {
            case "GupyScraper" -> 3;
            case "VagasScraper" -> 6;
            case "CathoScraper" -> 9;
            case "InfojobsScraper" -> 12;
            case "NinetyNineJobsScraper" -> 15;
            case "ProgramathorScraper" -> 18;
            case "TramposScraper" -> 21;
            case "IndeedScraper" -> 24;
            case "GlassdoorScraper" -> 27;
            case "NerdinScraper" -> 30;
            case "PicpayScraper" -> 33;
            case "IfoodScraper" -> 36;
            case "MercadoLivreScraper" -> 39;
            case "RemoteOkScraper" -> 42;
            case "WellfoundScraper" -> 45;
            case "OttaScraper" -> 48;
            case "WeWorkRemotelyScraper" -> 51;
            case "JobicyScraper" -> 54;
            case "RemotiveScraper" -> 57;
            case "normalizing" -> 80;
            case "deduplicating" -> 95;
            default -> 10;
        };
    }
}