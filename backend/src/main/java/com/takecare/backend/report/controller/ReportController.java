package com.takecare.backend.report.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.report.dto.CreatePatientReportRequestDTO;
import com.takecare.backend.report.dto.CreateReportRequestDTO;
import com.takecare.backend.report.dto.ReportResponseDTO;
import com.takecare.backend.report.service.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(
            @Valid @RequestBody CreateReportRequestDTO request
    ) {
        logger.info("POST /reports - sessionId={} specialistId={}",
                request.getSessionId(), request.getSpecialistId());

        try {
            ReportResponseDTO response = reportService.createReport(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("POST /reports - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (SecurityException e) {
            logger.warn("POST /reports - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("POST /reports - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            if (ReportService.getDuplicateReportMessage().equals(e.getMessage())) {
                logger.warn("POST /reports - duplicate: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }
            logger.warn("POST /reports - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("POST /reports - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/reports/patient")
    public ResponseEntity<?> createPatientReport(
            @Valid @RequestBody CreatePatientReportRequestDTO request
    ) {
        logger.info("POST /reports/patient - sessionId={} patientId={}",
                request.getSessionId(), request.getPatientId());

        try {
            ReportResponseDTO response = reportService.createPatientReport(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("POST /reports/patient - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (SecurityException e) {
            logger.warn("POST /reports/patient - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("POST /reports/patient - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            if (ReportService.getDuplicateReportMessage().equals(e.getMessage())) {
                logger.warn("POST /reports/patient - duplicate: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }
            logger.warn("POST /reports/patient - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("POST /reports/patient - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/session/{sessionId}/specialist/{specialistId} (ya existía)
    // -------------------------------------------------------------------------

    @GetMapping("/reports/session/{sessionId}/specialist/{specialistId}")
    public ResponseEntity<?> getReportBySession(
            @PathVariable Integer sessionId,
            @PathVariable Integer specialistId
    ) {
        logger.info("GET /reports/session/{}/specialist/{}", sessionId, specialistId);

        try {
            ReportResponseDTO response = reportService.getReportBySession(sessionId, specialistId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("GET /reports/session/specialist - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("GET /reports/session/specialist - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("GET /reports/session/specialist - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/session/{sessionId}/patient/{patientId}
    // -------------------------------------------------------------------------

    @GetMapping("/reports/session/{sessionId}/patient/{patientId}")
    public ResponseEntity<?> getPatientReportBySession(
            @PathVariable Integer sessionId,
            @PathVariable Integer patientId
    ) {
        logger.info("GET /reports/session/{}/patient/{}", sessionId, patientId);

        try {
            ReportResponseDTO response = reportService.getPatientReportBySession(sessionId, patientId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("GET /reports/session/patient - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("GET /reports/session/patient - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("GET /reports/session/patient - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/specialists/{specialistId}/reports
    // -------------------------------------------------------------------------

    @GetMapping("/specialists/{specialistId}/reports")
    public ResponseEntity<?> getReportsReceivedBySpecialist(
            @PathVariable Integer specialistId
    ) {
        logger.info("GET /specialists/{}/reports", specialistId);

        try {
            List<ReportResponseDTO> response = reportService.getReportsReceivedBySpecialist(specialistId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("GET /specialists/reports - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("GET /specialists/reports - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/patients/{patientId}/reports
    // -------------------------------------------------------------------------

    @GetMapping("/patients/{patientId}/reports")
    public ResponseEntity<?> getReportsReceivedByPatient(
            @PathVariable Integer patientId
    ) {
        logger.info("GET /patients/{}/reports", patientId);

        try {
            List<ReportResponseDTO> response = reportService.getReportsReceivedByPatient(patientId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("GET /patients/reports - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("GET /patients/reports - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
