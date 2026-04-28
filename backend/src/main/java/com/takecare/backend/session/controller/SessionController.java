package com.takecare.backend.session.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.takecare.backend.session.dto.CreateSessionRequestDTO;
import com.takecare.backend.session.dto.SessionResponseDTO;
import com.takecare.backend.session.service.SessionService;

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
}