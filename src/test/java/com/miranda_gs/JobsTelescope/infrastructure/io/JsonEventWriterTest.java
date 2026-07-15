package com.miranda_gs.JobsTelescope.infrastructure.io;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEventWriterTest {

    @Test
    void shouldWriteCompletedEvent() {
        var sw = new StringWriter();
        var writer = new JsonEventWriter(new PrintWriter(sw));

        writer.writeCompleted(42, "output/2026-07-14");

        var output = sw.toString();
        assertThat(output).contains("\"type\"");
        assertThat(output).contains("\"completed\"");
        assertThat(output).contains("42");
        assertThat(output).contains("output/2026-07-14");
    }

    @Test
    void shouldWriteProgressEvent() {
        var sw = new StringWriter();
        var writer = new JsonEventWriter(new PrintWriter(sw));

        writer.writeProgress("gupy", 50);

        var output = sw.toString();
        assertThat(output).contains("\"progress\"");
        assertThat(output).contains("\"gupy\"");
        assertThat(output).contains("50");
    }

    @Test
    void shouldWriteErrorEvent() {
        var sw = new StringWriter();
        var writer = new JsonEventWriter(new PrintWriter(sw));

        writer.writeError("Something went wrong");

        var output = sw.toString();
        assertThat(output).contains("\"error\"");
        assertThat(output).contains("Something went wrong");
    }
}