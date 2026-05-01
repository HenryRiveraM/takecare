package com.takecare.backend.session.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.takecare.backend.session.dto.AppointmentStatusResponseDto;
import com.takecare.backend.session.dto.CancelAppointmentRequestDTO;
import com.takecare.backend.session.dto.CreateSessionRequestDTO;
import com.takecare.backend.session.dto.SessionResponseDTO;
import com.takecare.backend.session.dto.UpdateAppointmentStatusRequestDto;
import com.takecare.backend.session.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({
        "/api/v1/appointments",
        "/api/v1/specialists/appointments"
})
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<?> createAppointment(
            @RequestBody CreateSessionRequestDTO request
    ) {
        logger.info("POST appointment - patientId={} scheduleId={} typeOfSession={}",
                request.getPatientId(), request.getScheduleId(), request.getTypeOfSession());

        try {
            SessionResponseDTO response = appointmentService.createAppointment(request);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("POST appointment - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SessionResponseDTO>> listByPatient(
            @PathVariable Integer patientId
    ) {
        logger.info("GET appointments by patientId={}", patientId);
        return ResponseEntity.ok(appointmentService.listByPatient(patientId));
    }

    @GetMapping("/specialist/{specialistId}")
    public ResponseEntity<List<SessionResponseDTO>> listBySpecialist(
            @PathVariable Integer specialistId
    ) {
        logger.info("GET appointments by specialistId={}", specialistId);
        return ResponseEntity.ok(appointmentService.listBySpecialist(specialistId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Integer id,
            @Valid @RequestBody CancelAppointmentRequestDTO request
    ) {
        logger.info("PATCH appointment cancel - id={} patientId={}",
                id, request.getPatientId());

        try {
            AppointmentStatusResponseDto response = appointmentService.cancelAppointment(
                    id,
                    request.getPatientId()
            );

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            logger.warn("PATCH appointment cancel - access denied or not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            logger.warn("PATCH appointment cancel - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("PATCH appointment cancel - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAppointmentStatusRequestDto request
    ) {
        logger.info("PATCH appointment status - id={} specialistId={} action={}",
                id, request.getSpecialistId(), request.getAction());

        try {
            AppointmentStatusResponseDto response = appointmentService.updateAppointmentStatus(
                    id,
                    request.getSpecialistId(),
                    request.getAction()
            );

            logger.info("PATCH appointment status completed - id={} status={}",
                    id, response.getStatus());

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            logger.warn("PATCH appointment status - access denied or not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            logger.warn("PATCH appointment status - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("PATCH appointment status - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}