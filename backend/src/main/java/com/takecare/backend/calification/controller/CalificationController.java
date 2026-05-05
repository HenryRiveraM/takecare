package com.takecare.backend.calification.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.calification.dto.CalificationResponseDTO;
import com.takecare.backend.calification.dto.CreateCalificationRequestDTO;
import com.takecare.backend.calification.service.CalificationService;
import com.takecare.backend.user.model.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sessions")
public class CalificationController {

    private static final Logger logger = LoggerFactory.getLogger(CalificationController.class);

    private final CalificationService calificationService;

    public CalificationController(CalificationService calificationService) {
        this.calificationService = calificationService;
    }

    @PostMapping("/{sessionId}/patient-ratings")
    public ResponseEntity<?> createPatientRating(
            @PathVariable Integer sessionId,
            @Valid @RequestBody CreateCalificationRequestDTO request,
            @AuthenticationPrincipal User requestingUser
    ) {
        logger.info("POST /api/v1/sessions/{}/patient-ratings - specialistId={}",
                sessionId, requestingUser != null ? requestingUser.getId() : "anonymous");

        try {
            CalificationResponseDTO response = calificationService.createPatientRating(
                    sessionId,
                    request,
                    requestingUser
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (NoSuchElementException e) {
            logger.warn("POST patient rating - session not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (AccessDeniedException e) {
            logger.warn("POST patient rating - access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (DuplicateKeyException e) {
            logger.warn("POST patient rating - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("POST patient rating - invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("POST patient rating - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado"));
        }
    }

    @GetMapping("/{sessionId}/patient-ratings")
    public ResponseEntity<?> getPatientRating(
            @PathVariable Integer sessionId,
            @AuthenticationPrincipal User requestingUser
    ) {
        logger.info("GET /api/v1/sessions/{}/patient-ratings - specialistId={}",
                sessionId, requestingUser != null ? requestingUser.getId() : "anonymous");

        try {
            CalificationResponseDTO response = calificationService.getPatientRating(
                    sessionId,
                    requestingUser
            );

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            logger.warn("GET patient rating - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (AccessDeniedException e) {
            logger.warn("GET patient rating - access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("GET patient rating - error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado"));
        }
    }
}
