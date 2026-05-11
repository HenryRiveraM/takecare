package com.takecare.backend.calification.controller;
 
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
 
import com.takecare.backend.calification.dto.CalificationResponseDTO;
import com.takecare.backend.calification.dto.CreateCalificationRequestDTO;
import com.takecare.backend.calification.dto.RatingSummaryDTO;
import com.takecare.backend.calification.service.CalificationService;
 
import jakarta.validation.Valid;
 
@RestController
public class CalificationController {
 
    private static final Logger logger = LoggerFactory.getLogger(CalificationController.class);
 
    private final CalificationService calificationService;
 
    public CalificationController(CalificationService calificationService) {
        this.calificationService = calificationService;
    }
 
    @PostMapping("/api/v1/sessions/{sessionId}/patient-ratings")
    public ResponseEntity<?> createPatientRating(
            @PathVariable Integer sessionId,
            @Valid @RequestBody CreateCalificationRequestDTO request
    ) {
        logger.info("POST /api/v1/sessions/{}/patient-ratings", sessionId);
 
        try {
            // TODO: obtener el usuario autenticado desde SecurityContext y pasarlo al servicio
            CalificationResponseDTO response = calificationService.createPatientRating(
                    sessionId, request, null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
 
        } catch (NoSuchElementException e) {
            logger.warn("POST patient rating - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (DuplicateKeyException e) {
            logger.warn("POST patient rating - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (AccessDeniedException e) {
            logger.warn("POST patient rating - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("POST patient rating - bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (RuntimeException e) {
            logger.error("POST patient rating - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado"));
        }
    }
 
    @GetMapping("/api/v1/sessions/{sessionId}/patient-ratings")
    public ResponseEntity<?> getPatientRating(
            @PathVariable Integer sessionId
    ) {
        logger.info("GET /api/v1/sessions/{}/patient-ratings", sessionId);
 
        try {
            // TODO: obtener el usuario autenticado desde SecurityContext
            CalificationResponseDTO response = calificationService.getPatientRating(
                    sessionId, null
            );
            return ResponseEntity.ok(response);
 
        } catch (NoSuchElementException e) {
            logger.warn("GET patient rating - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (AccessDeniedException e) {
            logger.warn("GET patient rating - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (RuntimeException e) {
            logger.error("GET patient rating - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado"));
        }
    }
 
    @PostMapping("/api/v1/sessions/{sessionId}/specialist-ratings")
    public ResponseEntity<?> createSpecialistRating(
            @PathVariable Integer sessionId,
            @Valid @RequestBody CreateCalificationRequestDTO request
    ) {
        logger.info("POST /api/v1/sessions/{}/specialist-ratings", sessionId);
 
        try {
            CalificationResponseDTO response = calificationService.createSpecialistRating(
                    sessionId, request, null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
 
        } catch (NoSuchElementException e) {
            logger.warn("POST specialist rating - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (DuplicateKeyException e) {
            logger.warn("POST specialist rating - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (AccessDeniedException e) {
            logger.warn("POST specialist rating - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("POST specialist rating - bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (RuntimeException e) {
            logger.error("POST specialist rating - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado"));
        }
    }

    @GetMapping("/api/v1/sessions/{sessionId}/specialist-ratings")
    public ResponseEntity<?> getSpecialistRating(
            @PathVariable Integer sessionId
    ) {
        logger.info("GET /api/v1/sessions/{}/specialist-ratings", sessionId);
 
        try {
            // TODO: obtener el usuario autenticado desde SecurityContext
            CalificationResponseDTO response = calificationService.getSpecialistRating(
                    sessionId, null
            );
            return ResponseEntity.ok(response);
 
        } catch (NoSuchElementException e) {
            logger.warn("GET specialist rating - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (AccessDeniedException e) {
            logger.warn("GET specialist rating - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
 
        } catch (RuntimeException e) {
            logger.error("GET specialist rating - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado"));
        }
    }
 
    @GetMapping("/api/v1/specialists/{specialistId}/ratings")
    public ResponseEntity<List<CalificationResponseDTO>> getRatingsBySpecialist(
            @PathVariable Integer specialistId
    ) {
        logger.info("GET /api/v1/specialists/{}/ratings", specialistId);
        List<CalificationResponseDTO> ratings = calificationService.getRatingsBySpecialist(specialistId);
        logger.info("GET ratings | specialistId={} | total={}", specialistId, ratings.size());
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/api/v1/specialists/{specialistId}/rating-summary")
    public ResponseEntity<RatingSummaryDTO> getRatingSummary(
            @PathVariable Integer specialistId
    ) {
        logger.info("GET /api/v1/specialists/{}/rating-summary", specialistId);
        RatingSummaryDTO summary = calificationService.getRatingSummary(specialistId);
        return ResponseEntity.ok(summary);
    }
 
    @GetMapping("/api/v1/patients/{patientId}/ratings")
    public ResponseEntity<List<CalificationResponseDTO>> getRatingsByPatient(
            @PathVariable Integer patientId
    ) {
        logger.info("GET /api/v1/patients/{}/ratings", patientId);
        List<CalificationResponseDTO> ratings = calificationService.getRatingsByPatient(patientId);
        logger.info("GET ratings | patientId={} | total={}", patientId, ratings.size());
        return ResponseEntity.ok(ratings);
    }
}