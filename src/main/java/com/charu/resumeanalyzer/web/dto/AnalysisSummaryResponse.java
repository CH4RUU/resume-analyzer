package com.charu.resumeanalyzer.web.dto;

import java.time.Instant;

import com.charu.resumeanalyzer.model.Analysis;

public record AnalysisSummaryResponse(
        Long id, String fileName, String jobTitle, int overallScore, Instant createdAt) {

    public static AnalysisSummaryResponse from(Analysis a) {
        return new AnalysisSummaryResponse(a.getId(), a.getFileName(), a.getJobTitle(), a.getOverallScore(),
                a.getCreatedAt());
    }
}
