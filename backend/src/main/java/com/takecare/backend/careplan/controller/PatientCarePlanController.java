package com.takecare.backend.careplan.controller;

import com.takecare.backend.careplan.dto.PatientCarePlanDetailDTO;
import com.takecare.backend.careplan.dto.PatientCarePlanListResponseDTO;
import com.takecare.backend.careplan.service.PatientCarePlanQueryService;
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
@RequestMapping("/api/v1/patients/{patientId}/care-plans")
public class PatientCarePlanController {

    private static final Logger logger = LoggerFactory.getLogger(PatientCarePlanController.class);

    private final PatientCarePlanQueryService patientCarePlanQueryService;

    public PatientCarePlanController(PatientCarePlanQueryService patientCarePlanQueryService) {
        this.patientCarePlanQueryService = patientCarePlanQueryService;
    }

    /**
     * Obtiene el plan de cuidado activo del paciente autenticado.
     *
     * <p>Retorna {@code 403} si el {@code patientId} no coincide con el header {@code X-Patient-Id}.</p>
     * <p>Retorna {@code 404} si el paciente no existe o no tiene plan activo.</p>
     *
     * @param patientId     ID del paciente (path variable).
     * @param sessionUserId ID del paciente extraído del token de sesión (header {@code X-Patient-Id}).
     * @return {@link PatientCarePlanDetailDTO} con el detalle del plan activo.
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCarePlan(
            @PathVariable Integer patientId,
            @RequestHeader("X-Patient-Id") Integer sessionUserId
    ) {
        logger.info("GET active care plan. patientId={}", patientId);

        try {
            PatientCarePlanDetailDTO response =
                    patientCarePlanQueryService.getActiveCarePlan(patientId, sessionUserId);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            logger.warn("GET active care plan - unauthorized access. patientId={}, sessionUserId={}",
                    patientId, sessionUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("GET active care plan - not found. patientId={}, reason={}", patientId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("GET active care plan - unexpected error. patientId={}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al consultar el plan de cuidado activo"));
        }
    }

    /**
     * Obtiene el historial completo de planes de cuidado del paciente autenticado.
     *
     * <p>Retorna {@code 403} si el {@code patientId} no coincide con el header {@code X-Patient-Id}.</p>
     * <p>Retorna {@code 404} si el paciente no existe.</p>
     *
     * @param patientId     ID del paciente (path variable).
     * @param sessionUserId ID del paciente extraído del token de sesión (header {@code X-Patient-Id}).
     * @return {@link PatientCarePlanListResponseDTO} con el historial de planes.
     */
    @GetMapping
    public ResponseEntity<?> getCarePlanHistory(
            @PathVariable Integer patientId,
            @RequestHeader("X-Patient-Id") Integer sessionUserId
    ) {
        logger.info("GET care plan history. patientId={}", patientId);

        try {
            PatientCarePlanListResponseDTO response =
                    patientCarePlanQueryService.getCarePlanHistory(patientId, sessionUserId);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            logger.warn("GET care plan history - unauthorized access. patientId={}, sessionUserId={}",
                    patientId, sessionUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (NoSuchElementException e) {
            logger.warn("GET care plan history - not found. patientId={}, reason={}", patientId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("GET care plan history - unexpected error. patientId={}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al consultar el historial de planes de cuidado"));
        }
    }
}
