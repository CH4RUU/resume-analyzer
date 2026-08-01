package com.charu.resumeanalyzer.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.charu.resumeanalyzer.model.Analysis;

class ResumeAnalyzerTest {

    private final ResumeAnalyzer analyzer = new ResumeAnalyzer(new SkillCatalog());

    @Test
    void scoresStrongResumeHighlyAgainstMatchingJobDescription() {
        String resume = """
                Jane Doe
                jane.doe@example.com | (555) 123-4567 | linkedin.com/in/janedoe

                Summary
                Backend engineer with 5 years building services in Java and Spring Boot.

                Experience
                - Led a team that built a Spring Boot microservice, reducing latency by 30%
                - Designed REST APIs backed by MySQL and Redis, improving throughput 2x
                - Implemented CI/CD pipeline with Docker and Jenkins, automating deployments

                Education
                B.S. Computer Science

                Skills
                Java, Spring Boot, MySQL, Docker, Kubernetes, Git, AWS
                """;
        String jobDescription = """
                We are looking for a backend engineer experienced with Java, Spring Boot,
                MySQL, Docker and AWS to build scalable REST APIs.
                """;

        Analysis result = analyzer.analyze(resume, jobDescription, "resume.txt", "txt");

        assertThat(result.getMatchedSkills()).contains("Java", "Spring Boot", "MySQL", "Docker", "AWS");
        assertThat(result.getMissingSkills()).isEmpty();
        assertThat(result.getKeywordScore()).isEqualTo(100);
        assertThat(result.getContactScore()).isEqualTo(100);
        assertThat(result.getOverallScore()).isGreaterThan(70);
    }

    @Test
    void flagsMissingSkillsAndWeakContactInfo() {
        String resume = "I am a hard worker who wants a job. I did stuff at a company.";
        String jobDescription = "Looking for a Python developer with AWS and Docker experience.";

        Analysis result = analyzer.analyze(resume, jobDescription, "resume.txt", "txt");

        assertThat(result.getMissingSkills()).contains("Python", "AWS", "Docker");
        assertThat(result.getContactScore()).isEqualTo(0);
        assertThat(result.getSuggestions()).isNotEmpty();
    }
}
