package com.miranda_gs.JobsTelescope.domain.entity;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SearchResult {
    SearchRequest request;
    List<Job> jobs;
    @Builder.Default
    int totalFound = 0;
    @Builder.Default
    int totalDeduplicated = 0;
    String outputPath;
}