package com.miranda_gs.JobsTelescope.infrastructure.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class JsonCommandReader {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BufferedReader reader;

    public JsonCommandReader() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public JsonCommandReader(BufferedReader reader) {
        this.reader = reader;
    }

    public SearchRequest readCommand() throws Exception {
        var line = reader.readLine();
        if (line == null || line.isBlank()) {
            return null;
        }
        var node = mapper.readTree(line);
        var command = node.get("command").asText();

        var builder = SearchRequest.builder();

        if (node.has("query")) {
            builder.query(node.get("query").asText());
        }
        if (node.has("region")) {
            builder.region(com.miranda_gs.JobsTelescope.domain.entity.Region.valueOf(
                    node.get("region").asText()));
        }
        if (node.has("platforms") && node.get("platforms").isArray()) {
            var platforms = new java.util.ArrayList<com.miranda_gs.JobsTelescope.domain.entity.Platform>();
            for (var p : node.get("platforms")) {
                platforms.add(com.miranda_gs.JobsTelescope.domain.entity.Platform.valueOf(p.asText()));
            }
            builder.platforms(platforms);
        }
        if (node.has("maxResults")) {
            builder.maxResults(node.get("maxResults").asInt());
        }

        return builder.build();
    }
}