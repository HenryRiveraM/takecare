package com.takecare.backend.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.service.PatientService;
import com.takecare.backend.user.service.SpecialistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final PatientService patientService;
    private final SpecialistService specialistService;

    public UserController(PatientService patientService, SpecialistService specialistService) {
        this.patientService = patientService;
        this.specialistService = specialistService;
    }

    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegisterDTO dto) {
        logger.info("POST /api/v1/users/register/patient - Registering patient with email: {}", dto.getEmail());
        patientService.registerPatientFromDTO(dto);
        logger.info("POST /api/v1/users/register/patient - Patient registered successfully");
        return ResponseEntity.ok("Patient registered");
    }

    @PostMapping("/register/specialist")
    public ResponseEntity<?> registerSpecialist(@Valid @RequestBody SpecialistRegisterDTO dto) {
        logger.info("POST /api/v1/users/register/specialist - Registering specialist with email: {}", dto.getEmail());
        specialistService.registerSpecialistFromDTO(dto);
        logger.info("POST /api/v1/users/register/specialist - Specialist registered successfully");
        return ResponseEntity.ok("Specialist registered");
    }
}