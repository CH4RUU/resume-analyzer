package com.charu.resumeanalyzer.web.dto;

import java.time.Instant;
import java.util.List;

import com.charu.resumeanalyzer.model.Analysis;

public record AnalysisResponse(
        Long id,
        String fileName,
        String fileType,
        String jobTitle,
        int overallScore,
        int keywordScore,
        int sectionScore,
        int contactScore,
        int impactScore,
        int formatScore,
        int wordCount,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> suggestions,
        String aiFeedback,
        Instant createdAt) {

    public static AnalysisResponse from(Analysis a) {
        return new AnalysisResponse(
                a.getId(), a.getFileName(), a.getFileType(), a.getJobTitle(),
                a.getOverallScore(), a.getKeywordScore(), a.getSectionScore(),
                a.getContactScore(), a.getImpactScore(), a.getFormatScore(),
                a.getWordCount(), a.getMatchedSkills(), a.getMissingSkills(),
                a.getSuggestions(), a.getAiFeedback(), a.getCreatedAt());
    }
}
