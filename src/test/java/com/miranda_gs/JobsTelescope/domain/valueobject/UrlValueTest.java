package com.miranda_gs.JobsTelescope.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValueTest {

    @Test
    void shouldCreateValidUrl() {
        var url = new Url("https://example.com/jobs/123");
        assertThat(url.getValue()).isEqualTo("https://example.com/jobs/123");
    }

    @Test
    void shouldTrimWhitespace() {
        var url = new Url("  https://example.com  ");
        assertThat(url.getValue()).isEqualTo("https://example.com");
    }

    @Test
    void shouldRejectBlankUrl() {
        assertThatThrownBy(() -> new Url(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }
}