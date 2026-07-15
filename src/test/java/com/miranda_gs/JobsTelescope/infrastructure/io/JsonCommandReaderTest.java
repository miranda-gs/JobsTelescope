package com.miranda_gs.JobsTelescope.infrastructure.io;

import com.miranda_gs.JobsTelescope.domain.entity.Region;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class JsonCommandReaderTest {

    @Test
    void shouldParseSearchCommand() throws Exception {
        var json = "{\"command\":\"search\",\"query\":\"Java Backend\",\"region\":\"BRAZIL\",\"platforms\":[\"GUPY\"],\"maxResults\":10}";
        var reader = new JsonCommandReader(new BufferedReader(new StringReader(json)));

        var request = reader.readCommand();

        assertThat(request.getQuery()).isEqualTo("Java Backend");
        assertThat(request.getRegion()).isEqualTo(Region.BRAZIL);
        assertThat(request.getPlatforms()).hasSize(1);
        assertThat(request.getMaxResults()).isEqualTo(10);
    }

    @Test
    void shouldReturnNullOnEmptyInput() throws Exception {
        var reader = new JsonCommandReader(new BufferedReader(new StringReader("")));
        assertThat(reader.readCommand()).isNull();
    }
}