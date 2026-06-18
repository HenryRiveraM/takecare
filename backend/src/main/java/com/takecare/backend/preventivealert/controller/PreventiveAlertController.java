package com.takecare.backend.preventivealert.controller;

import com.takecare.backend.preventivealert.dto.PreventiveAlertResponseDTO;
import com.takecare.backend.preventivealert.service.PreventiveAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1")
public class PreventiveAlertController {

    private static final Logger logger = LoggerFactory.getLogger(PreventiveAlertController.class);

    private final PreventiveAlertService preventiveAlertService;

    public PreventiveAlertController(PreventiveAlertService preventiveAlertService) {
        this.preventiveAlertService = preventiveAlertService;
    }

    @GetMapping("/specialists/{specialistId}/preventive-alerts")
    public ResponseEntity<?> getAlertsBySpecialist(
            @PathVariable Integer specialistId,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole
    ) {
        logger.info("GET /api/v1/specialists/{}/preventive-alerts. sessionUserId={}, sessionUserRole={}",
                specialistId, sessionUserId, sessionUserRole);

        if (sessionUserId != null && sessionUserRole != null) {
            if (!"SPECIALIST".equalsIgnoreCase(sessionUserRole) || !specialistId.equals(sessionUserId)) {
                logger.warn("Unauthorized GET preventive-alerts. specialistId={}, sessionUserId={}", specialistId, sessionUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acceso denegado: no autorizado"));
            }
        }

        try {
            List<PreventiveAlertResponseDTO> alerts = preventiveAlertService.getAlertsBySpecialist(specialistId);
            return ResponseEntity.ok(alerts);
        } catch (NoSuchElementException e) {
            logger.warn("GET preventive-alerts - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("GET preventive-alerts - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al obtener alertas preventivas"));
        }
    }

    @PatchMapping("/preventive-alerts/{alertId}/reviewed")
    public ResponseEntity<?> markAsReviewed(
            @PathVariable Long alertId,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole
    ) {
        logger.info("PATCH /api/v1/preventive-alerts/{}/reviewed. sessionUserId={}, sessionUserRole={}",
                alertId, sessionUserId, sessionUserRole);

        if (sessionUserId == null) {
            logger.warn("Unauthorized PATCH preventive-alerts reviewed. Missing X-User-Id header.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Identificador de usuario faltante en la cabecera"));
        }

        if (sessionUserRole != null && !"SPECIALIST".equalsIgnoreCase(sessionUserRole)) {
            logger.warn("Unauthorized PATCH preventive-alerts reviewed. sessionUserRole={}", sessionUserRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acceso denegado: rol no autorizado"));
        }

        try {
            PreventiveAlertResponseDTO response = preventiveAlertService.markAsReviewed(alertId, sessionUserId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("PATCH preventive-alerts reviewed - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("PATCH preventive-alerts reviewed - unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al marcar alerta como revisada"));
        }
    }
}
