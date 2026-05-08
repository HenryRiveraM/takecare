package com.takecare.backend.report.controller;

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

import com.takecare.backend.report.dto.CreateReportRequestDTO;
import com.takecare.backend.report.dto.ReportResponseDTO;
import com.takecare.backend.report.service.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestBody CreateReportRequestDTO request
    ) {
        logger.info("POST report - sessionId={} specialistId={}",
            request.getSessionId(),
            request.getSpecialistId());

        try {
            ReportResponseDTO response = reportService.createReport(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("POST report - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (SecurityException e) {
            logger.warn("POST report - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("POST report - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            if (ReportService.getDuplicateReportMessage().equals(e.getMessage())) {
                logger.warn("POST report - duplicate: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }

            logger.warn("POST report - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("POST report - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}/specialist/{specialistId}")
    public ResponseEntity<?> getReportBySession(
            @PathVariable Integer sessionId,
            @PathVariable Integer specialistId
    ) {
        logger.info("GET report - sessionId={} specialistId={}", sessionId, specialistId);

        try {
            ReportResponseDTO response = reportService.getReportBySession(sessionId, specialistId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("GET report - invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("GET report - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("GET report - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
