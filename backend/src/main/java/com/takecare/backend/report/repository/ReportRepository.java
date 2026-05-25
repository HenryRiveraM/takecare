package com.takecare.backend.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.takecare.backend.report.model.Report;

@Repository
public interface ReportRepository
        extends JpaRepository<Report, Integer> {

    boolean existsBySessionIdAndReporterIdAndReportedId(
            Integer sessionId,
            Integer reporterId,
            Integer reportedId
    );

    Optional<Report> findBySessionIdAndReporterId(
            Integer sessionId,
            Integer reporterId
    );

    Optional<Report> findBySessionIdAndReportedId(
            Integer sessionId,
            Integer reportedId
    );
    
    List<Report> findAllByReportedId(Integer reportedId);
}
