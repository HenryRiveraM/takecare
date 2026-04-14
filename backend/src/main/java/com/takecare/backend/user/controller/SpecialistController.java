package com.takecare.backend.user.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.takecare.backend.specialities.dto.SpecialistFilterResponseDTO;
import com.takecare.backend.specialities.service.SpecialistSearchService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.user.dto.SpecialistLocationResponseDto;
import com.takecare.backend.user.dto.SpecialistProfileDTO;
import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.dto.UpdateSpecialistLocationRequestDto;
import com.takecare.backend.user.dto.UpdateSpecialistProfileDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.service.SpecialistProfileService;
import com.takecare.backend.user.service.SpecialistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/specialists")
public class SpecialistController {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistController.class);

    private final SpecialistService specialistService;
    private final SpecialistProfileService specialistProfileService;
    private final SpecialistSearchService specialistSearchService;

    public SpecialistController(SpecialistService specialistService,
                                SpecialistProfileService specialistProfileService,
                                SpecialistSearchService specialistSearchService) {
        this.specialistService = specialistService;
        this.specialistProfileService = specialistProfileService;
        this.specialistSearchService = specialistSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SpecialistFilterResponseDTO>> searchSpecialists(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String availability) {
        logger.info("GET /api/v1/specialists/search | category={} | availability={}", category, availability);
        List<SpecialistFilterResponseDTO> result = specialistSearchService.searchSpecialists(category, availability);
        logger.info("GET /api/v1/specialists/search | total={}", result.size());
        return ResponseEntity.ok(result);
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

    @GetMapping("/{id}/profile")
    public ResponseEntity<SpecialistProfileDTO> getSpecialistProfile(@PathVariable Integer id) {
        try {
            logger.info("GET /api/v1/specialists/{}/profile - Fetching specialist profile", id);
            return specialistProfileService.getProfile(id)
                    .map(profile -> {
                        logger.info("GET /api/v1/specialists/{}/profile - Specialist profile found", id);
                        return ResponseEntity.ok(profile);
                    })
                    .orElseGet(() -> {
                        logger.warn("GET /api/v1/specialists/{}/profile - Specialist profile not found", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (RuntimeException e) {
            logger.error("GET /api/v1/specialists/{}/profile - Error fetching specialist profile", id, e);
            throw e;
        } catch (Exception e) {
            logger.error("GET /api/v1/specialists/{}/profile - Unexpected error fetching specialist profile", id, e);
            throw new RuntimeException("Error al obtener perfil del especialista");
        }
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

    @PutMapping("/{id}/profile")
    public ResponseEntity<SpecialistProfileDTO> updateSpecialistProfile(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSpecialistProfileDTO dto) {
        try {
            logger.info("PUT /api/v1/specialists/{}/profile - Updating specialist profile", id);
            return specialistProfileService.updateProfile(id, dto)
                    .map(profile -> {
                        logger.info("PUT /api/v1/specialists/{}/profile - Specialist profile updated successfully", id);
                        return ResponseEntity.ok(profile);
                    })
                    .orElseGet(() -> {
                        logger.warn("PUT /api/v1/specialists/{}/profile - Specialist profile not found for update", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (RuntimeException e) {
            logger.error("PUT /api/v1/specialists/{}/profile - Error updating specialist profile", id, e);
            throw e;
        } catch (Exception e) {
            logger.error("PUT /api/v1/specialists/{}/profile - Unexpected error updating specialist profile", id, e);
            throw new RuntimeException("Error al actualizar perfil del especialista");
        }
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateSpecialistLocation(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSpecialistLocationRequestDto dto) {
        try {
            logger.info("PUT /api/v1/specialists/{}/location - Updating specialist location", id);
            SpecialistLocationResponseDto response =
                specialistProfileService.updateSpecialistLocation(id, id, dto);
            logger.info("PUT /api/v1/specialists/{}/location - Specialist location updated successfully", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("PUT /api/v1/specialists/{}/location - Invalid location data", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("PUT /api/v1/specialists/{}/location - Forbidden location update", id, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            logger.warn("PUT /api/v1/specialists/{}/location - Specialist not found", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("PUT /api/v1/specialists/{}/location - Error updating specialist location", id, e);
            throw e;
        } catch (Exception e) {
            logger.error("PUT /api/v1/specialists/{}/location - Unexpected error updating specialist location", id, e);
            throw new RuntimeException("Error al actualizar ubicación del especialista");
        }
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