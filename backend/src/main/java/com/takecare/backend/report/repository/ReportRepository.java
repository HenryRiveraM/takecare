package com.takecare.backend.report.repository;

import com.takecare.backend.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository 
        extends JpaRepository<Report, Integer> {

    boolean existsBySessionIdAndReporterIdAndReportedId(
            Integer sessionId,
            Integer reporterId,
            Integer reportedId
    );

}