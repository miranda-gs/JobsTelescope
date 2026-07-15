package com.miranda_gs.JobsTelescope.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRequestTest {

    @Test
    void shouldCreateSearchRequestWithDefaults() {
        var request = SearchRequest.builder()
                .query("Java Backend")
                .region(Region.BRAZIL)
                .build();

        assertThat(request.getQuery()).isEqualTo("Java Backend");
        assertThat(request.getRegion()).isEqualTo(Region.BRAZIL);
        assertThat(request.getPlatforms()).isEmpty();
        assertThat(request.getMaxResults()).isNull();
    }

    @Test
    void shouldCreateSearchRequestWithAllFields() {
        var request = SearchRequest.builder()
                .query("Python Developer")
                .region(Region.INTERNATIONAL)
                .platforms(List.of(Platform.REMOTE_OK, Platform.WELLFOUND))
                .maxResults(10)
                .build();

        assertThat(request.getPlatforms()).hasSize(2);
        assertThat(request.getMaxResults()).isEqualTo(10);
    }
}