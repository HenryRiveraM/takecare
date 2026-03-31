package com.takecare.backend.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.user.dto.PatientProfileDTO;
import com.takecare.backend.user.dto.UpdatePatientProfileDTO;
import com.takecare.backend.user.service.PatientProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/profile")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    private final PatientProfileService patientProfileService;

    public UserProfileController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientProfileDTO> getProfile(@PathVariable Integer id) {
        logger.info("GET /api/v1/users/profile/{} - Fetching patient profile", id);
        PatientProfileDTO profile = patientProfileService.getProfile(id);
        logger.info("GET /api/v1/users/profile/{} - Profile found", id);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientProfileDTO> updateProfile(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePatientProfileDTO dto) {
        logger.info("PUT /api/v1/users/profile/{} - Updating patient profile", id);
        PatientProfileDTO updated = patientProfileService.updateProfile(id, dto);
        logger.info("PUT /api/v1/users/profile/{} - Profile updated successfully", id);
        return ResponseEntity.ok(updated);
    }
}
