package com.charu.resumeanalyzer.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * A single stored resume analysis. One row per uploaded resume.
 */
@Entity
@Table(name = "analysis")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 32)
    private String fileType;

    @Column(length = 255)
    private String jobTitle;

    @Column(nullable = false)
    private int overallScore;

    private int keywordScore;
    private int sectionScore;
    private int contactScore;
    private int impactScore;
    private int formatScore;

    private int wordCount;

    @Lob
    @Column(name = "resume_text", columnDefinition = "LONGTEXT")
    private String resumeText;

    @Lob
    @Column(name = "ai_feedback", columnDefinition = "LONGTEXT")
    private String aiFeedback;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_matched_skill", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "skill", length = 100)
    private List<String> matchedSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_missing_skill", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "skill", length = 100)
    private List<String> missingSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_suggestion", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "suggestion", length = 500)
    private List<String> suggestions = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Analysis() {
        // for JPA
    }

    public Analysis(String fileName, String fileType, String jobTitle) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.jobTitle = jobTitle;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public int getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(int keywordScore) {
        this.keywordScore = keywordScore;
    }

    public int getSectionScore() {
        return sectionScore;
    }

    public void setSectionScore(int sectionScore) {
        this.sectionScore = sectionScore;
    }

    public int getContactScore() {
        return contactScore;
    }

    public void setContactScore(int contactScore) {
        this.contactScore = contactScore;
    }

    public int getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(int impactScore) {
        this.impactScore = impactScore;
    }

    public int getFormatScore() {
        return formatScore;
    }

    public void setFormatScore(int formatScore) {
        this.formatScore = formatScore;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
