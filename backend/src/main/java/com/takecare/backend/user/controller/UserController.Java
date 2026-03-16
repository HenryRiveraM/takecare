package com.takecare.backend.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.service.PatientService;
import com.takecare.backend.user.service.SpecialistService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final PatientService patientService;
    private final SpecialistService specialistService;

    public UserController(PatientService patientService, SpecialistService specialistService) {
        this.patientService = patientService;
        this.specialistService = specialistService;
    }

    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegisterDTO dto) {
        patientService.registerPatientFromDTO(dto);
        return ResponseEntity.ok("Patient registered");
    }

    @PostMapping("/register/specialist")
    public ResponseEntity<?> registerSpecialist(@Valid @RequestBody SpecialistRegisterDTO dto) {
        specialistService.registerSpecialistFromDTO(dto);
        return ResponseEntity.ok("Specialist registered");
    }

}