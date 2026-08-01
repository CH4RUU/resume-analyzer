package com.charu.resumeanalyzer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.charu.resumeanalyzer.model.Analysis;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    List<Analysis> findAllByOrderByCreatedAtDesc();
}
