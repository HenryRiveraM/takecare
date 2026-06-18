package com.takecare.backend.careplan.controller;

import com.takecare.backend.careplan.dto.EmotionalRecordRequestDTO;
import com.takecare.backend.careplan.dto.EmotionalRecordResponseDTO;
import com.takecare.backend.careplan.service.EmotionalRecordService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1")
public class EmotionalRecordController {

    private static final Logger logger = LoggerFactory.getLogger(EmotionalRecordController.class);

    private final EmotionalRecordService emotionalRecordService;

    public EmotionalRecordController(EmotionalRecordService emotionalRecordService) {
        this.emotionalRecordService = emotionalRecordService;
    }

    @PostMapping("/patients/{patientId}/emotional-records")
    public ResponseEntity<?> createRecord(
            @PathVariable Integer patientId,
            @RequestHeader(value = "X-Patient-Id", required = false) Integer sessionPatientId,
            @Valid @RequestBody EmotionalRecordRequestDTO request
    ) {
        logger.info("POST emotional-records. patientId={}, sessionPatientId={}", patientId, sessionPatientId);

        if (sessionPatientId != null && !patientId.equals(sessionPatientId)) {
            logger.warn("Unauthorized POST emotional-records. patientId={}, sessionPatientId={}", patientId, sessionPatientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acceso denegado: no autorizado"));
        }

        try {
            EmotionalRecordResponseDTO response = emotionalRecordService.createRecord(patientId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            logger.warn("POST emotional-records - duplicate day: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            logger.warn("POST emotional-records - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("POST emotional-records - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("POST emotional-records - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al registrar estado emocional"));
        }
    }

    @GetMapping("/patients/{patientId}/emotional-records")
    public ResponseEntity<?> getRecords(
            @PathVariable Integer patientId,
            @RequestHeader(value = "X-Patient-Id", required = false) Integer sessionPatientId
    ) {
        logger.info("GET emotional-records. patientId={}, sessionPatientId={}", patientId, sessionPatientId);

        if (sessionPatientId != null && !patientId.equals(sessionPatientId)) {
            logger.warn("Unauthorized GET emotional-records. patientId={}, sessionPatientId={}", patientId, sessionPatientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acceso denegado: no autorizado"));
        }

        try {
            List<EmotionalRecordResponseDTO> response = emotionalRecordService.getRecords(patientId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET emotional-records - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("GET emotional-records - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET emotional-records - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al obtener registros emocionales"));
        }
    }

    @GetMapping("/specialists/{specialistId}/patients/{patientId}/emotional-records")
    public ResponseEntity<?> getRecordsForSpecialist(
            @PathVariable Integer specialistId,
            @PathVariable Integer patientId,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole
    ) {
        logger.info("GET emotional-records for specialist. specialistId={}, patientId={}, sessionUserId={}, sessionUserRole={}",
                specialistId, patientId, sessionUserId, sessionUserRole);

        if (sessionUserId != null && sessionUserRole != null) {
            if (!"SPECIALIST".equalsIgnoreCase(sessionUserRole) || !specialistId.equals(sessionUserId)) {
                logger.warn("Unauthorized GET emotional-records for specialist. specialistId={}, sessionUserId={}", specialistId, sessionUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acceso denegado: no autorizado"));
            }
        }

        try {
            List<EmotionalRecordResponseDTO> response = emotionalRecordService.getRecordsForSpecialist(specialistId, patientId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET emotional-records for specialist - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("GET emotional-records for specialist - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET emotional-records for specialist - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al obtener registros emocionales"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Datos invalidos")
                .orElse("Datos invalidos");
        logger.warn("Emotional records validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }
}
