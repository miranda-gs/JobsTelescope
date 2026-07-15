package com.miranda_gs.JobsTelescope.domain.port;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;

import java.util.List;

public interface JobScraper {
    List<Job> search(SearchRequest request);
}