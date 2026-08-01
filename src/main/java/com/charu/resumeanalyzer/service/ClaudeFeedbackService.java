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
 * Only active when an Anthropic API key is configured; otherwise every call fails fast
 * with {@link AiNotConfiguredException} so callers can degrade gracefully.
 */
@Service
public class ClaudeFeedbackService {

    private static final int MAX_RESUME_CHARS = 6000;
    private static final int MAX_JOB_DESCRIPTION_CHARS = 3000;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public ClaudeFeedbackService(
            @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-sonnet-5}") String model) {
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
                    "AI feedback is not configured on this server. Set the ANTHROPIC_API_KEY environment variable to enable it.");
        }

        String prompt = buildPrompt(analysis, jobDescription);

        MessageRequest request = new MessageRequest(model, 700,
                List.of(new MessageRequest.Message("user", prompt)));

        MessageResponse response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(MessageResponse.class);
        } catch (Exception e) {
            throw new BadRequestException("Failed to reach the AI feedback service: " + e.getMessage());
        }

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new BadRequestException("The AI feedback service returned an empty response.");
        }
        return response.content().get(0).text();
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

    private record MessageRequest(String model, int max_tokens, List<Message> messages) {
        private record Message(String role, String content) {
        }
    }

    private record MessageResponse(List<ContentBlock> content) {
        private record ContentBlock(String type, String text) {
        }
    }
}
