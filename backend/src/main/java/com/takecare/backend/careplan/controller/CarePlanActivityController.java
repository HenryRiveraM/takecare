package com.takecare.backend.careplan.controller;

import com.takecare.backend.careplan.dto.CarePlanActivityListResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanActivityProgressResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanActivityRequestDTO;
import com.takecare.backend.careplan.dto.CarePlanItemResponseDTO;
import com.takecare.backend.careplan.dto.UpdateCarePlanItemRequestDTO;
import com.takecare.backend.careplan.service.CarePlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1")
public class CarePlanActivityController {

    private static final Logger logger = LoggerFactory.getLogger(CarePlanActivityController.class);

    private final CarePlanService carePlanService;

    public CarePlanActivityController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    @PostMapping("/care-plans/{planId}/activities")
    public ResponseEntity<?> createActivity(
            @PathVariable Long planId,
            @RequestParam Integer specialistId,
            @Valid @RequestBody CarePlanActivityRequestDTO request
    ) {
        logger.info("POST care plan activity. planId={}, specialistId={}", planId, specialistId);

        try {
            CarePlanItemResponseDTO response = carePlanService.createActivity(planId, specialistId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            logger.warn("POST care plan activity - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("POST care plan activity - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("POST care plan activity - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("POST care plan activity - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al crear la actividad"));
        }
    }

    @GetMapping("/care-plans/{planId}/activities")
    public ResponseEntity<?> listActivities(
            @PathVariable Long planId,
            @RequestParam(required = false) Integer specialistId,
            @RequestParam(required = false) Integer patientId
    ) {
        logger.info("GET care plan activities. planId={}, specialistId={}, patientId={}",
                planId, specialistId, patientId);

        try {
            CarePlanActivityListResponseDTO response = carePlanService.listActivities(planId, specialistId, patientId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET care plan activities - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("GET care plan activities - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET care plan activities - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al consultar actividades"));
        }
    }

    @PatchMapping("/care-plan-activities/{activityId}")
    public ResponseEntity<?> updateActivity(
            @PathVariable Long activityId,
            @RequestParam Integer specialistId,
            @Valid @RequestBody UpdateCarePlanItemRequestDTO request
    ) {
        logger.info("PATCH care plan activity. activityId={}, specialistId={}", activityId, specialistId);

        try {
            CarePlanItemResponseDTO response = carePlanService.updateActivity(activityId, specialistId, request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan activity - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan activity - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("PATCH care plan activity - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan activity - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al actualizar la actividad"));
        }
    }

    @PatchMapping("/care-plan-activities/{activityId}/complete")
    public ResponseEntity<?> completeActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) Integer specialistId
    ) {
        logger.info("PATCH care plan activity complete. activityId={}, patientId={}, specialistId={}",
                activityId, patientId, specialistId);

        try {
            CarePlanActivityProgressResponseDTO response =
                    carePlanService.completeActivity(activityId, patientId, specialistId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan activity complete - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan activity complete - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan activity complete - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al completar la actividad"));
        }
    }

    @PatchMapping("/care-plan-activities/{activityId}/pending")
    public ResponseEntity<?> markActivityPending(
            @PathVariable Long activityId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) Integer specialistId
    ) {
        logger.info("PATCH care plan activity pending. activityId={}, patientId={}, specialistId={}",
                activityId, patientId, specialistId);

        try {
            CarePlanActivityProgressResponseDTO response =
                    carePlanService.markActivityPending(activityId, patientId, specialistId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan activity pending - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan activity pending - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan activity pending - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al marcar la actividad pendiente"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Datos invalidos")
                .orElse("Datos invalidos");
        logger.warn("Care plan activity validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }
}
