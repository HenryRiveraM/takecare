package com.takecare.backend.session.controller;

import com.takecare.backend.session.dto.AppointmentStatusResponseDto;
import com.takecare.backend.session.dto.UpdateAppointmentStatusRequestDto;
import com.takecare.backend.session.service.AppointmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/specialists/appointments")
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * PATCH /api/v1/specialists/appointments/{id}/status
     *
     * Permite al especialista dueño de la cita aprobar o rechazar una solicitud pendiente.
     *
     * Body esperado:
     * {
     *   "specialistId": 5,
     *   "action": "accept"   // o "reject"
     * }
     *
     * Respuestas:
     *  200 OK              → cita actualizada correctamente
     *  400 Bad Request     → acción inválida o cita no está en estado pending
     *  403 Forbidden       → specialistId no es dueño de la cita
     *  404 Not Found       → cita no existe
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAppointmentStatusRequestDto request) {

        logger.info("PATCH /api/v1/specialists/appointments/{}/status - specialistId={} action={}",
                id, request.getSpecialistId(), request.getAction());

        try {
            AppointmentStatusResponseDto response = appointmentService.updateAppointmentStatus(
                    id,
                    request.getSpecialistId(),
                    request.getAction()
            );

            logger.info("PATCH /api/v1/specialists/appointments/{}/status - completed successfully, status={}",
                    id, response.getStatus());

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            // Cita no encontrada O no pertenece al especialista → 403 para no revelar existencia
            logger.warn("PATCH /appointments/{}/status - access denied or not found: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            // Cita no está en estado PENDING
            logger.warn("PATCH /appointments/{}/status - invalid state: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("PATCH /appointments/{}/status - unexpected error", id, e);
            throw e;
        }
    }
}
