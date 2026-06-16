package com.takecare.backend.careplan.controller;

import com.takecare.backend.careplan.dto.CarePlanListResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanItemRequestDTO;
import com.takecare.backend.careplan.dto.CarePlanItemResponseDTO;
import com.takecare.backend.careplan.dto.CreateCarePlanRequestDTO;
import com.takecare.backend.careplan.dto.UpdateCarePlanItemRequestDTO;
import com.takecare.backend.careplan.dto.UpdateCarePlanRequestDTO;
import com.takecare.backend.careplan.service.CarePlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class CarePlanController {

    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private final CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    @PostMapping("/specialists/{specialistId}/patients/{patientId}/care-plans")
    public ResponseEntity<?> createCarePlan(
            @PathVariable Integer specialistId,
            @PathVariable Integer patientId,
            @Valid @RequestBody CreateCarePlanRequestDTO request
    ) {
        logger.info("POST care plan. specialistId={}, patientId={}", specialistId, patientId);

        try {
            CarePlanResponseDTO response = carePlanService.createCarePlan(specialistId, patientId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            logger.warn("POST care plan - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("POST care plan - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("POST care plan - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("POST care plan - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("POST care plan - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al crear el plan"));
        }
    }

    @GetMapping("/specialists/{specialistId}/patients/{patientId}/care-plans")
    public ResponseEntity<?> listCarePlans(
            @PathVariable Integer specialistId,
            @PathVariable Integer patientId
    ) {
        logger.info("GET care plans. specialistId={}, patientId={}", specialistId, patientId);

        try {
            CarePlanListResponseDTO response = carePlanService.listCarePlans(specialistId, patientId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET care plans - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("GET care plans - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET care plans - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al consultar planes"));
        }
    }

    @GetMapping("/specialists/{specialistId}/care-plans")
    public ResponseEntity<?> listSpecialistCarePlans(
            @PathVariable Integer specialistId
    ) {
        logger.info("GET care plans by specialist. specialistId={}", specialistId);

        try {
            CarePlanListResponseDTO response = carePlanService.listCarePlansBySpecialist(specialistId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET specialist care plans - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET specialist care plans - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al consultar planes"));
        }
    }

    @GetMapping("/care-plans/{planId}")
    public ResponseEntity<?> getCarePlan(
            @PathVariable Long planId,
            @RequestParam(required = false) Integer specialistId,
            @RequestParam(required = false) Integer patientId
    ) {
        logger.info("GET care plan detail. planId={}, specialistId={}, patientId={}",
                planId, specialistId, patientId);

        try {
            CarePlanResponseDTO response = carePlanService.getCarePlan(planId, specialistId, patientId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET care plan detail - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("GET care plan detail - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET care plan detail - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al consultar el plan"));
        }
    }

    @PatchMapping("/care-plans/{planId}")
    public ResponseEntity<?> updateCarePlan(
            @PathVariable Long planId,
            @RequestParam(required = false) Integer specialistId,
            @Valid @RequestBody UpdateCarePlanRequestDTO request
    ) {
        logger.info("PATCH care plan. planId={}, specialistId={}", planId, specialistId);

        try {
            CarePlanResponseDTO response = carePlanService.updateCarePlan(planId, specialistId, request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("PATCH care plan - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("PATCH care plan - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al actualizar el plan"));
        }
    }

    @DeleteMapping("/care-plans/{planId}")
    public ResponseEntity<?> deleteCarePlan(
            @PathVariable Long planId,
            @RequestParam(required = false) Integer specialistId
    ) {
        logger.info("DELETE care plan. planId={}, specialistId={}", planId, specialistId);

        try {
            carePlanService.deleteCarePlan(planId, specialistId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            logger.warn("DELETE care plan - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("DELETE care plan - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("DELETE care plan - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al eliminar el plan"));
        }
    }

    @PatchMapping("/care-plans/{planId}/archive")
    public ResponseEntity<?> archiveCarePlan(
            @PathVariable Long planId,
            @RequestParam(required = false) Integer specialistId
    ) {
        logger.info("PATCH archive care plan. planId={}, specialistId={}", planId, specialistId);

        try {
            carePlanService.archiveCarePlan(planId, specialistId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Plan archivado correctamente"
            ));
        } catch (NoSuchElementException e) {
            logger.warn("PATCH archive care plan - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH archive care plan - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("PATCH archive care plan - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH archive care plan - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al archivar el plan"));
        }
    }

    @PostMapping("/care-plans/{planId}/items")
    public ResponseEntity<?> addCarePlanItem(
            @PathVariable Long planId,
            @RequestParam(required = false) Integer specialistId,
            @Valid @RequestBody CarePlanItemRequestDTO request
    ) {
        logger.info("POST care plan item. planId={}, specialistId={}", planId, specialistId);

        try {
            CarePlanItemResponseDTO response = carePlanService.addItem(planId, specialistId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            logger.warn("POST care plan item - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("POST care plan item - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("POST care plan item - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("POST care plan item - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al crear actividad"));
        }
    }

    @PatchMapping("/care-plan-items/{itemId}")
    public ResponseEntity<?> updateCarePlanItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Integer specialistId,
            @Valid @RequestBody UpdateCarePlanItemRequestDTO request
    ) {
        logger.info("PATCH care plan item. itemId={}, specialistId={}", itemId, specialistId);

        try {
            CarePlanItemResponseDTO response = carePlanService.updateItem(itemId, specialistId, request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan item - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan item - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("PATCH care plan item - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan item - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al actualizar actividad"));
        }
    }

    @PatchMapping("/care-plan-items/{itemId}/complete")
    public ResponseEntity<?> completeCarePlanItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) Integer specialistId
    ) {
        logger.info("PATCH care plan item complete. itemId={}, patientId={}, specialistId={}",
                itemId, patientId, specialistId);

        try {
            CarePlanResponseDTO response = carePlanService.completeItem(itemId, patientId, specialistId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan item complete - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan item complete - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan item complete - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al completar actividad"));
        }
    }

    @PatchMapping("/care-plan-items/{itemId}/pending")
    public ResponseEntity<?> pendingCarePlanItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) Integer specialistId
    ) {
        logger.info("PATCH care plan item pending. itemId={}, patientId={}, specialistId={}",
                itemId, patientId, specialistId);

        try {
            CarePlanResponseDTO response = carePlanService.markItemPending(itemId, patientId, specialistId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH care plan item pending - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PATCH care plan item pending - forbidden: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PATCH care plan item pending - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al marcar actividad pendiente"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Datos invalidos")
                .orElse("Datos invalidos");
        logger.warn("Care plan validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }
}
