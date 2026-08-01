package com.charu.resumeanalyzer.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.charu.resumeanalyzer.model.Analysis;
import com.charu.resumeanalyzer.repository.AnalysisRepository;
import com.charu.resumeanalyzer.web.BadRequestException;

@Service
public class AnalysisService {

    private final TextExtractor textExtractor;
    private final ResumeAnalyzer resumeAnalyzer;
    private final ClaudeFeedbackService claudeFeedbackService;
    private final AnalysisRepository repository;

    public AnalysisService(TextExtractor textExtractor, ResumeAnalyzer resumeAnalyzer,
            ClaudeFeedbackService claudeFeedbackService, AnalysisRepository repository) {
        this.textExtractor = textExtractor;
        this.resumeAnalyzer = resumeAnalyzer;
        this.claudeFeedbackService = claudeFeedbackService;
        this.repository = repository;
    }

    @Transactional
    public Analysis analyzeAndStore(MultipartFile resumeFile, String jobDescription) {
        if (resumeFile == null || resumeFile.isEmpty()) {
            throw new BadRequestException("Please attach a resume file (PDF, DOCX or TXT).");
        }
        String text = textExtractor.extract(resumeFile);
        String extension = textExtractor.extensionOf(
                resumeFile.getOriginalFilename() == null ? "" : resumeFile.getOriginalFilename());
        Analysis analysis = resumeAnalyzer.analyze(text, jobDescription, resumeFile.getOriginalFilename(),
                extension);
        return repository.save(analysis);
    }

    public List<Analysis> history() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Analysis get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No analysis found with id " + id));
    }

    @Transactional
    public Analysis generateAiFeedback(Long id, String jobDescription) {
        Analysis analysis = get(id);
        String feedback = claudeFeedbackService.generateFeedback(analysis, jobDescription);
        analysis.setAiFeedback(feedback);
        return repository.save(analysis);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("No analysis found with id " + id);
        }
        repository.deleteById(id);
    }
}
