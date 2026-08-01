package com.charu.resumeanalyzer.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.charu.resumeanalyzer.model.Analysis;
import com.charu.resumeanalyzer.web.AiNotConfiguredException;
import com.charu.resumeanalyzer.web.BadRequestException;

/**
 * Optional AI-powered feedback layer on top of the rule-based {@link ResumeAnalyzer}.
 * Only active when a Gemini API key is configured; otherwise every call fails fast
 * with {@link AiNotConfiguredException} so callers can degrade gracefully.
 */
@Service
public class GeminiFeedbackService {

    private static final int MAX_RESUME_CHARS = 6000;
    private static final int MAX_JOB_DESCRIPTION_CHARS = 3000;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiFeedbackService(
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateFeedback(Analysis analysis, String jobDescription) {
        if (!isEnabled()) {
            throw new AiNotConfiguredException(
                    "AI feedback is not configured on this server. Set the GEMINI_API_KEY environment variable to enable it.");
        }

        String prompt = buildPrompt(analysis, jobDescription);

        GenerateContentRequest request = new GenerateContentRequest(
                List.of(new GenerateContentRequest.Content(
                        List.of(new GenerateContentRequest.Content.Part(prompt)))));

        GenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .header("content-type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(GenerateContentResponse.class);
        } catch (Exception e) {
            throw new BadRequestException("Failed to reach the AI feedback service: " + e.getMessage());
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new BadRequestException("The AI feedback service returned an empty response.");
        }
        List<GenerateContentResponse.Candidate.Content.Part> parts =
                response.candidates().get(0).content().parts();
        if (parts == null || parts.isEmpty()) {
            throw new BadRequestException("The AI feedback service returned an empty response.");
        }
        return parts.get(0).text();
    }

    private String buildPrompt(Analysis analysis, String jobDescription) {
        String resumeText = truncate(analysis.getResumeText(), MAX_RESUME_CHARS);
        String jd = jobDescription == null || jobDescription.isBlank()
                ? "(no job description provided)"
                : truncate(jobDescription, MAX_JOB_DESCRIPTION_CHARS);

        return """
                You are a career coach reviewing a resume. Give direct, specific, actionable feedback.

                RESUME:
                %s

                JOB DESCRIPTION:
                %s

                AUTOMATED SCORING ALREADY COMPUTED (for context only, do not just repeat it):
                - Overall score: %d/100
                - Matched skills: %s
                - Missing skills: %s

                Write feedback as short paragraphs or bullet points covering: the resume's strongest points,
                its biggest weaknesses, and 3-5 concrete edits the candidate should make. Keep it under 300 words.
                """.formatted(resumeText, jd, analysis.getOverallScore(),
                String.join(", ", analysis.getMatchedSkills()),
                String.join(", ", analysis.getMissingSkills()));
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }

    private record GenerateContentRequest(List<Content> contents) {
        private record Content(List<Part> parts) {
            private record Part(String text) {
            }
        }
    }

    private record GenerateContentResponse(List<Candidate> candidates) {
        private record Candidate(Content content) {
            private record Content(List<Part> parts) {
                private record Part(String text) {
                }
            }
        }
    }
}
