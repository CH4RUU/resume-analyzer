package com.charu.resumeanalyzer.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.charu.resumeanalyzer.model.Analysis;
import com.charu.resumeanalyzer.service.AnalysisService;
import com.charu.resumeanalyzer.web.dto.AnalysisResponse;
import com.charu.resumeanalyzer.web.dto.AnalysisSummaryResponse;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestPart("resume") MultipartFile resume,
            @RequestParam(value = "jobDescription", required = false, defaultValue = "") String jobDescription) {
        Analysis analysis = analysisService.analyzeAndStore(resume, jobDescription);
        return ResponseEntity.ok(AnalysisResponse.from(analysis));
    }

    @GetMapping
    public List<AnalysisSummaryResponse> history() {
        return analysisService.history().stream().map(AnalysisSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(@PathVariable Long id) {
        return AnalysisResponse.from(analysisService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        analysisService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ai-feedback")
    public AnalysisResponse generateAiFeedback(@PathVariable Long id,
            @RequestParam(value = "jobDescription", required = false, defaultValue = "") String jobDescription) {
        return AnalysisResponse.from(analysisService.generateAiFeedback(id, jobDescription));
    }
}
