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

import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.service.PatientService;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        logger.info("GET /api/v1/patients - Fetching all patients");
        List<Patient> patients = patientService.getAllPatients();
        logger.info("GET /api/v1/patients - Found {} patients", patients.size());
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Integer id) {
        logger.info("GET /api/v1/patients/{} - Fetching patient by id", id);
        return patientService.getPatientById(id)
                .map(patient -> {
                    logger.info("GET /api/v1/patients/{} - Patient found", id);
                    return ResponseEntity.ok(patient);
                })
                .orElseGet(() -> {
                    logger.warn("GET /api/v1/patients/{} - Patient not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Patient> registerPatient(@RequestBody PatientRegisterDTO patientDTO) {
        logger.info("POST /api/v1/patients - Registering new patient with email: {}", patientDTO.getEmail());
        Patient createdPatient = patientService.registerPatientFromDTO(patientDTO);
        logger.info("POST /api/v1/patients - Patient registered successfully with id: {}", createdPatient.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPatient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Integer id, @RequestBody Patient patientDetails) {
        logger.info("PUT /api/v1/patients/{} - Updating patient", id);
        return patientService.updatePatient(id, patientDetails)
                .map(updated -> {
                    logger.info("PUT /api/v1/patients/{} - Patient updated successfully", id);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    logger.warn("PUT /api/v1/patients/{} - Patient not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Integer id) {
        logger.info("DELETE /api/v1/patients/{} - Deleting patient", id);
        if (patientService.deletePatient(id)) {
            logger.info("DELETE /api/v1/patients/{} - Patient deleted successfully", id);
            return ResponseEntity.noContent().build();
        }
        logger.warn("DELETE /api/v1/patients/{} - Patient not found for deletion", id);
        return ResponseEntity.notFound().build();
    }
}