package com.takecare.backend.careplan.controller;

import com.takecare.backend.careplan.dto.CreateLogbookNoteRequestDTO;
import com.takecare.backend.careplan.dto.LogbookNoteResponseDTO;
import com.takecare.backend.careplan.service.TrackingNoteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/care-plans/{planId}/logbook")
public class TrackingNoteController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingNoteController.class);

    private final TrackingNoteService trackingNoteService;

    public TrackingNoteController(TrackingNoteService trackingNoteService) {
        this.trackingNoteService = trackingNoteService;
    }

    @PostMapping
    public ResponseEntity<?> addNote(
            @PathVariable Long planId,
            @Valid @RequestBody CreateLogbookNoteRequestDTO request
    ) {
        logger.info("POST /care-plans/{}/logbook - received note request", planId);

        try {
            LogbookNoteResponseDTO response = trackingNoteService.addNote(planId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            logger.warn("POST /care-plans/{}/logbook - not found: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("POST /care-plans/{}/logbook - unexpected error", planId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al guardar la nota de bitácora"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getNotes(@PathVariable Long planId) {
        logger.info("GET /care-plans/{}/logbook - fetching notes", planId);

        try {
            List<LogbookNoteResponseDTO> response = trackingNoteService.getNotesByPlan(planId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET /care-plans/{}/logbook - not found: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET /care-plans/{}/logbook - unexpected error", planId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al obtener notas de bitácora"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Datos invalidos")
                .orElse("Datos invalidos");
        logger.warn("Logbook validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }
}
