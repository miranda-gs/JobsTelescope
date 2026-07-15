package com.miranda_gs.JobsTelescope.infrastructure.normalizer;

import com.miranda_gs.JobsTelescope.domain.entity.Job;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

public class JobDeduplicator {

    private final double similarityThreshold;

    public JobDeduplicator(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public List<Job> deduplicate(List<Job> jobs) {
        var unique = new LinkedHashMap<String, Job>();

        for (var job : jobs) {
            var dedupKey = buildDedupKey(job);
            if (!unique.containsKey(dedupKey)) {
                unique.put(dedupKey, job);
            }
        }
        return List.copyOf(unique.values());
    }

    private String buildDedupKey(Job job) {
        return (job.getTitle().getValue() + "|" + job.getCompany())
                .toLowerCase()
                .replaceAll("[^a-z0-9|]", "");
    }
}