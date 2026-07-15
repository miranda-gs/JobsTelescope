package com.miranda_gs.JobsTelescope.infrastructure.io;

import com.miranda_gs.JobsTelescope.application.usecase.SearchJobs;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.error.InvalidSearchRequestException;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;

public class CommandProcessor {

    private final JsonCommandReader reader;
    private final JsonEventWriter writer;
    private final SearchJobs searchJobs;
    private final InfrastructureLogger log = new InfrastructureLogger(CommandProcessor.class);

    public CommandProcessor() {
        this.reader = new JsonCommandReader();
        this.writer = new JsonEventWriter();
        this.searchJobs = new SearchJobs();
    }

    public CommandProcessor(JsonCommandReader reader, JsonEventWriter writer, SearchJobs searchJobs) {
        this.reader = reader;
        this.writer = writer;
        this.searchJobs = searchJobs;
    }

    public boolean processNext() throws Exception {
        SearchRequest request;
        try {
            request = reader.readCommand();
        } catch (IllegalArgumentException e) {
            writer.writeError("Invalid command: " + e.getMessage());
            return true;
        }

        if (request == null) return false;

        log.info("Processing command: query={}, region={}", request.getQuery(), request.getRegion());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            writer.writeError("Query is required");
            return true;
        }
        if (request.getRegion() == null) {
            writer.writeError("Region is required");
            return true;
        }

        try {
            searchJobs.execute(request, writer);
        } catch (Exception e) {
            log.error("Command failed", e);
            writer.writeError(e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
        return true;
    }
}