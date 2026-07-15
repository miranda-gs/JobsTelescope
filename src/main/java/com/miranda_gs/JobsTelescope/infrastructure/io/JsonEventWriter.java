package com.miranda_gs.JobsTelescope.infrastructure.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;

public class JsonEventWriter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PrintWriter writer;

    public JsonEventWriter() {
        this.writer = new PrintWriter(System.out, true);
    }

    public JsonEventWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void writeCompleted(int jobsFound, String outputPath) {
        var event = mapper.createObjectNode();
        event.put("type", "completed");
        event.put("jobsFound", jobsFound);
        event.put("output", outputPath);
        writer.println(mapper.valueToTree(event));
        writer.flush();
    }

    public void writeProgress(String platform, int percentage) {
        var event = mapper.createObjectNode();
        event.put("type", "progress");
        event.put("platform", platform);
        event.put("percentage", percentage);
        writer.println(mapper.valueToTree(event));
        writer.flush();
    }

    public void writeError(String message) {
        var event = mapper.createObjectNode();
        event.put("type", "error");
        event.put("message", message);
        writer.println(mapper.valueToTree(event));
        writer.flush();
    }
}