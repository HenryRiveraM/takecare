package com.takecare.backend.session.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.takecare.backend.session.dto.CancelSessionRequestDTO;
import com.takecare.backend.session.dto.CreateSessionRequestDTO;
import com.takecare.backend.session.dto.SessionResponseDTO;
import com.takecare.backend.session.dto.SessionStatusResponseDTO;
import com.takecare.backend.session.dto.UpdateSessionStatusRequestDTO;
import com.takecare.backend.session.service.SessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({
        "/api/v1/sessions",

        // Rutas temporales para no romper frontend antiguo
        "/api/v1/appointments",
        "/api/v1/specialists/appointments"
})
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<?> createSession(
            @RequestBody CreateSessionRequestDTO request
    ) {
        logger.info("POST session - patientId={} scheduleId={} typeOfSession={}",
                request.getPatientId(), request.getScheduleId(), request.getTypeOfSession());

        try {
            SessionResponseDTO response = sessionService.createSession(request);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("POST session - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SessionResponseDTO>> listByPatient(
            @PathVariable Integer patientId
    ) {
        logger.info("GET sessions by patientId={}", patientId);
        return ResponseEntity.ok(sessionService.listByPatient(patientId));
    }

    @GetMapping("/specialist/{specialistId}")
    public ResponseEntity<List<SessionResponseDTO>> listBySpecialist(
            @PathVariable Integer specialistId
    ) {
        logger.info("GET sessions by specialistId={}", specialistId);
        return ResponseEntity.ok(sessionService.listBySpecialist(specialistId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSession(
            @PathVariable Integer id,
            @Valid @RequestBody CancelSessionRequestDTO request
    ) {
        logger.info("PATCH session cancel - id={} patientId={}",
                id, request.getPatientId());

        try {
            SessionStatusResponseDTO response = sessionService.cancelSession(
                    id,
                    request.getPatientId()
            );

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            logger.warn("PATCH session cancel - access denied or not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            logger.warn("PATCH session cancel - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("PATCH session cancel - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateSessionStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSessionStatusRequestDTO request
    ) {
        logger.info("PATCH session status - id={} specialistId={} action={}",
                id, request.getSpecialistId(), request.getAction());

        try {
            SessionStatusResponseDTO response = sessionService.updateSessionStatus(
                    id,
                    request.getSpecialistId(),
                    request.getAction()
            );

            logger.info("PATCH session status completed - id={} status={}",
                    id, response.getStatus());

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            logger.warn("PATCH session status - access denied or not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            logger.warn("PATCH session status - invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.warn("PATCH session status - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}