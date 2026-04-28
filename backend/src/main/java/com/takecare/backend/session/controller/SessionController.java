package com.takecare.backend.session.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.takecare.backend.session.dto.AppointmentStatusResponseDto;
import com.takecare.backend.session.dto.CreateSessionRequestDTO;
import com.takecare.backend.session.dto.SessionResponseDTO;
import com.takecare.backend.session.dto.UpdateAppointmentStatusRequestDto;
import com.takecare.backend.session.service.SessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(
            @RequestBody CreateSessionRequestDTO request
    ) {
        return ResponseEntity.ok(sessionService.createSession(request));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SessionResponseDTO>> listByPatient(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(sessionService.listByPatient(patientId));
    }

    @GetMapping("/specialist/{specialistId}")
    public ResponseEntity<List<SessionResponseDTO>> listBySpecialist(
            @PathVariable Integer specialistId
    ) {
        return ResponseEntity.ok(sessionService.listBySpecialist(specialistId));
    }

    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<?> updateSessionStatus(
            @PathVariable Integer sessionId,
            @Valid @RequestBody UpdateAppointmentStatusRequestDto request
    ) {
        try {
            AppointmentStatusResponseDto response = sessionService.updateAppointmentStatus(
                    sessionId,
                    request.getSpecialistId(),
                    request.getAction()
            );

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}