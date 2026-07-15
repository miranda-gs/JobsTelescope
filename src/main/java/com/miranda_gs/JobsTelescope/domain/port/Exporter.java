package com.miranda_gs.JobsTelescope.domain.port;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.SearchResult;

import java.util.List;

public interface Exporter {
    void export(SearchResult result, List<Job> jobs);
}