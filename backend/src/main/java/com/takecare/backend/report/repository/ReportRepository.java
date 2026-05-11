package com.takecare.backend.report.repository;

import com.takecare.backend.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    // GET /api/v1/reports/session/{sessionId}/patient/{patientId}
    Optional<Report> findBySessionIdAndReportedId(
            Integer sessionId,
            Integer reportedId
    );

    // GET /api/v1/specialists/{specialistId}/reports  — reportes recibidos por el especialista
    // GET /api/v1/patients/{patientId}/reports        — reportes recibidos por el paciente
    // La diferencia semántica (quién es specialist vs patient) la resuelve el servicio
    List<Report> findAllByReportedId(Integer reportedId);
}
