package com.miranda_gs.JobsTelescope.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobTitleValueTest {

    @Test
    void shouldCreateValidJobTitle() {
        var title = new JobTitle("Java Backend Developer");
        assertThat(title.getValue()).isEqualTo("Java Backend Developer");
    }

    @Test
    void shouldTrimWhitespace() {
        var title = new JobTitle("  Senior Dev  ");
        assertThat(title.getValue()).isEqualTo("Senior Dev");
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThatThrownBy(() -> new JobTitle(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job title");
    }
}