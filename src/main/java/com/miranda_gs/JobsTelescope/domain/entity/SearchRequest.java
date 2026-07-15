package com.miranda_gs.JobsTelescope.domain.entity;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SearchRequest {
    String query;
    Region region;
    @Builder.Default
    List<Platform> platforms = List.of();
    Integer maxResults;
}