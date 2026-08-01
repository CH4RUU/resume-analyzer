package com.charu.resumeanalyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.charu.resumeanalyzer.model.Analysis;

/**
 * Scores extracted resume text, optionally against a job description, and
 * produces the persisted {@link Analysis} result.
 */
@Service
public class ResumeAnalyzer {

    private static final List<String> SECTION_HEADINGS = List.of(
            "experience", "work experience", "employment", "education", "skills",
            "projects", "certifications", "summary", "objective");

    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d{1,3}[ .-]?)?(\\(?\\d{3}\\)?[ .-]?)\\d{3}[ .-]?\\d{4}");
    private static final Pattern LINKEDIN = Pattern.compile("linkedin\\.com/\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITHUB = Pattern.compile("github\\.com/\\S+", Pattern.CASE_INSENSITIVE);

    private static final List<String> IMPACT_VERBS = List.of(
            "led", "built", "designed", "developed", "implemented", "architected", "optimized",
            "improved", "reduced", "increased", "launched", "automated", "migrated", "delivered",
            "created", "managed", "mentored", "drove", "scaled", "streamlined");

    private static final Pattern NUMBER_OR_PERCENT = Pattern.compile("\\b\\d+[%x]?\\b");
    private static final Pattern BULLET_LINE = Pattern.compile("^\\s*([\\-*••]|\\d+[.)])\\s+");

    private final SkillCatalog skillCatalog;

    public ResumeAnalyzer(SkillCatalog skillCatalog) {
        this.skillCatalog = skillCatalog;
    }

    public Analysis analyze(String resumeText, String jobDescription, String fileName, String fileType) {
        String jobTitleGuess = jobDescription == null || jobDescription.isBlank() ? null : "Target role";
        Analysis analysis = new Analysis(fileName, fileType, jobTitleGuess);
        analysis.setResumeText(resumeText);
        analysis.setWordCount(countWords(resumeText));

        Set<String> resumeSkills = skillCatalog.findIn(resumeText);

        boolean hasJobDescription = jobDescription != null && !jobDescription.isBlank();
        Set<String> targetSkills = hasJobDescription ? skillCatalog.findIn(jobDescription) : resumeSkills;

        Set<String> matched = new LinkedHashSet<>(resumeSkills);
        matched.retainAll(targetSkills);
        Set<String> missing = new LinkedHashSet<>(targetSkills);
        missing.removeAll(resumeSkills);

        analysis.setMatchedSkills(new ArrayList<>(matched));
        analysis.setMissingSkills(new ArrayList<>(missing));

        int keywordScore = scoreKeywords(targetSkills, matched, hasJobDescription, resumeSkills);
        int sectionScore = scoreSections(resumeText);
        int contactScore = scoreContactInfo(resumeText);
        int impactScore = scoreImpact(resumeText);
        int formatScore = scoreFormat(resumeText);

        analysis.setKeywordScore(keywordScore);
        analysis.setSectionScore(sectionScore);
        analysis.setContactScore(contactScore);
        analysis.setImpactScore(impactScore);
        analysis.setFormatScore(formatScore);

        int overall = Math.round(
                keywordScore * 0.35f + sectionScore * 0.2f + contactScore * 0.1f
                        + impactScore * 0.2f + formatScore * 0.15f);
        analysis.setOverallScore(overall);

        analysis.setSuggestions(buildSuggestions(analysis, hasJobDescription));

        return analysis;
    }

    private int scoreKeywords(Set<String> targetSkills, Set<String> matched, boolean hasJobDescription,
            Set<String> resumeSkills) {
        if (!hasJobDescription) {
            // No JD to match against: score on breadth of recognized skills instead.
            return Math.min(100, resumeSkills.size() * 8);
        }
        if (targetSkills.isEmpty()) {
            return 100;
        }
        return Math.round(matched.size() * 100f / targetSkills.size());
    }

    private int scoreSections(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        long found = SECTION_HEADINGS.stream().filter(lower::contains).count();
        int distinctCoreSections = 0;
        if (lower.contains("experience") || lower.contains("employment")) {
            distinctCoreSections++;
        }
        if (lower.contains("education")) {
            distinctCoreSections++;
        }
        if (lower.contains("skills")) {
            distinctCoreSections++;
        }
        int coverage = Math.min(100, (int) (found * 100 / SECTION_HEADINGS.size()) + distinctCoreSections * 10);
        return Math.min(100, coverage);
    }

    private int scoreContactInfo(String text) {
        int score = 0;
        if (EMAIL.matcher(text).find()) {
            score += 40;
        }
        if (PHONE.matcher(text).find()) {
            score += 30;
        }
        if (LINKEDIN.matcher(text).find() || GITHUB.matcher(text).find()) {
            score += 30;
        }
        return Math.min(100, score);
    }

    private int scoreImpact(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        long verbHits = IMPACT_VERBS.stream().filter(v -> lower.contains(v)).count();
        Matcher numberMatcher = NUMBER_OR_PERCENT.matcher(text);
        int numberHits = 0;
        while (numberMatcher.find() && numberHits < 10) {
            numberHits++;
        }
        int verbScore = Math.min(60, (int) verbHits * 8);
        int numberScore = Math.min(40, numberHits * 6);
        return Math.min(100, verbScore + numberScore);
    }

    private int scoreFormat(String text) {
        String[] lines = text.split("\\r?\\n");
        long bulletLines = 0;
        long nonEmptyLines = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            nonEmptyLines++;
            if (BULLET_LINE.matcher(line).find()) {
                bulletLines++;
            }
        }
        int wordCount = countWords(text);
        int lengthScore;
        if (wordCount < 150) {
            lengthScore = 40;
        } else if (wordCount <= 1100) {
            lengthScore = 100;
        } else {
            lengthScore = 60;
        }
        int bulletRatio = nonEmptyLines == 0 ? 0 : (int) (bulletLines * 100 / nonEmptyLines);
        int bulletScore = Math.min(100, bulletRatio * 3);
        return Math.round(lengthScore * 0.6f + bulletScore * 0.4f);
    }

    private List<String> buildSuggestions(Analysis analysis, boolean hasJobDescription) {
        List<String> suggestions = new ArrayList<>();

        if (hasJobDescription && !analysis.getMissingSkills().isEmpty()) {
            int show = Math.min(8, analysis.getMissingSkills().size());
            suggestions.add("Add or highlight these job-relevant skills if you have them: "
                    + String.join(", ", analysis.getMissingSkills().subList(0, show)));
        }
        if (analysis.getContactScore() < 70) {
            suggestions.add("Make sure your email, phone number, and a LinkedIn/GitHub link are clearly visible near the top.");
        }
        if (analysis.getSectionScore() < 70) {
            suggestions.add("Add clear section headings such as Experience, Education, and Skills so ATS systems can parse your resume.");
        }
        if (analysis.getImpactScore() < 50) {
            suggestions.add("Use strong action verbs (led, built, improved) and quantify results with numbers or percentages, e.g. 'reduced load time by 30%'.");
        }
        if (analysis.getFormatScore() < 60) {
            suggestions.add("Use concise bullet points instead of long paragraphs, and aim for roughly 400-900 words total.");
        }
        if (analysis.getWordCount() < 150) {
            suggestions.add("Your resume looks quite short — add more detail about your experience and projects.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Solid resume overall — consider tailoring keywords to each specific job description before applying.");
        }
        return suggestions;
    }

    private int countWords(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }
}
