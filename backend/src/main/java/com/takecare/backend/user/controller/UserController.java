package com.takecare.backend.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.auth.DTO.ApiResponseDTO;
import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
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
        Patient patient = patientService.registerPatientFromDTO(dto);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, patient, null));
    }

    @PostMapping("/register/specialist")
    public ResponseEntity<?> registerSpecialist(@Valid @RequestBody SpecialistRegisterDTO dto) {
        Specialist specialist = specialistService.registerSpecialistFromDTO(dto);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, specialist, null));
    }

}