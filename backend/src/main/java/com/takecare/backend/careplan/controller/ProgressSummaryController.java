package com.takecare.backend.careplan.controller;

import com.takecare.backend.careplan.dto.ProgressSummaryDTO;
import com.takecare.backend.careplan.service.ProgressSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/care-plans")
public class ProgressSummaryController {

    private static final Logger logger = LoggerFactory.getLogger(ProgressSummaryController.class);

    private final ProgressSummaryService progressSummaryService;

    public ProgressSummaryController(ProgressSummaryService progressSummaryService) {
        this.progressSummaryService = progressSummaryService;
    }

    @GetMapping("/{planId}/progress-summary")
    public ResponseEntity<?> getProgressSummary(
            @PathVariable Long planId,
            @RequestHeader("X-User-Id") Integer requestingUserId,
            @RequestHeader("X-User-Role") String role
    ) {
        logger.info("GET progress summary. planId={}, requestingUserId={}, role={}", planId, requestingUserId, role);

        try {
            ProgressSummaryDTO response = progressSummaryService.getSummary(planId, requestingUserId, role);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            logger.warn("GET progress summary - unauthorized. planId={}, requestingUserId={}", planId, requestingUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("GET progress summary - not found. planId={}, reason={}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("GET progress summary - unexpected error. planId={}", planId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al calcular el resumen de progreso"));
        }
    }
}
