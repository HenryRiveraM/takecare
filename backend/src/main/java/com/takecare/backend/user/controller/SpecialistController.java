package com.takecare.backend.user.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.service.SpecialistService;

@RestController
@RequestMapping("/api/v1/specialists")
public class SpecialistController {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistController.class);

    private final SpecialistService specialistService;

    public SpecialistController(SpecialistService specialistService) {
        this.specialistService = specialistService;
    }

    @GetMapping
    public ResponseEntity<List<Specialist>> getAllSpecialists() {
        logger.info("GET /api/v1/specialists - Fetching all specialists");
        List<Specialist> specialists = specialistService.getAllSpecialists();
        logger.info("GET /api/v1/specialists - Found {} specialists", specialists.size());
        return ResponseEntity.ok(specialists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Specialist> getSpecialistById(@PathVariable Integer id) {
        logger.info("GET /api/v1/specialists/{} - Fetching specialist by id", id);
        return specialistService.getSpecialistById(id)
                .map(specialist -> {
                    logger.info("GET /api/v1/specialists/{} - Specialist found", id);
                    return ResponseEntity.ok(specialist);
                })
                .orElseGet(() -> {
                    logger.warn("GET /api/v1/specialists/{} - Specialist not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Specialist> registerSpecialist(@RequestBody SpecialistRegisterDTO specialistDTO) {
        logger.info("POST /api/v1/specialists - Registering new specialist with email: {}", specialistDTO.getEmail());
        Specialist createdSpecialist = specialistService.registerSpecialistFromDTO(specialistDTO);
        logger.info("POST /api/v1/specialists - Specialist registered successfully with id: {}", createdSpecialist.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSpecialist);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Specialist> updateSpecialist(@PathVariable Integer id, @RequestBody Specialist specialistDetails) {
        logger.info("PUT /api/v1/specialists/{} - Updating specialist", id);
        return specialistService.updateSpecialist(id, specialistDetails)
                .map(updated -> {
                    logger.info("PUT /api/v1/specialists/{} - Specialist updated successfully", id);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    logger.warn("PUT /api/v1/specialists/{} - Specialist not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecialist(@PathVariable Integer id) {
        logger.info("DELETE /api/v1/specialists/{} - Deleting specialist", id);
        if (specialistService.deleteSpecialist(id)) {
            logger.info("DELETE /api/v1/specialists/{} - Specialist deleted successfully", id);
            return ResponseEntity.noContent().build();
        }
        logger.warn("DELETE /api/v1/specialists/{} - Specialist not found for deletion", id);
        return ResponseEntity.notFound().build();
    }
}
