package com.takecare.backend.user.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.user.dto.AdminPatientDTO;
import com.takecare.backend.user.dto.AdminSpecialistDTO;
import com.takecare.backend.user.dto.VerifyUserRequest;
import com.takecare.backend.user.dto.VerifyUserResponse;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;
import com.takecare.backend.user.service.PatientService;
import com.takecare.backend.user.service.SpecialistService;
import com.takecare.backend.user.service.UserVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final int ROLE_ADMIN = 3;

    private final PatientService patientService;
    private final SpecialistService specialistService;
    private final UserRepository userRepository;
    private final UserVerificationService userVerificationService;

    public AdminController(PatientService patientService,
                           SpecialistService specialistService,
                           UserRepository userRepository,
                           UserVerificationService userVerificationService) {
        this.patientService = patientService;
        this.specialistService = specialistService;
        this.userRepository = userRepository;
        this.userVerificationService = userVerificationService;
    }

    private void validateAdminRole(Integer adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (user.getRole() == null || user.getRole() != ROLE_ADMIN) {
            throw new RuntimeException("Acceso denegado: solo administradores pueden acceder");
        }
    }

    public static class UpdateUserStatusRequest {
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

    @GetMapping("/patients")
    public ResponseEntity<List<AdminPatientDTO>> getAllPatients(@RequestHeader("X-Admin-Id") Integer adminId) {
        validateAdminRole(adminId);
        List<AdminPatientDTO> patients = patientService.getAllPatients()
            .stream()
            .map(this::toAdminPatientDTO)
            .toList();
        return ResponseEntity.ok(patients);
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<Void> deletePatient(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId) {
        validateAdminRole(adminId);
        boolean deleted = patientService.deletePatient(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/patients/{id}/validate/approve")
    public ResponseEntity<Patient> approvePatient(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/patients/{}/validate/approve - approving patient", id);
        validateAdminRole(adminId);

        return patientService.validatePatient(id, true)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/patients/{id}/validate/reject")
    public ResponseEntity<Patient> rejectPatient(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/patients/{}/validate/reject - rejecting patient", id);
        validateAdminRole(adminId);

        return patientService.validatePatient(id, false)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/specialists")
    public ResponseEntity<List<AdminSpecialistDTO>> getAllSpecialists(@RequestHeader("X-Admin-Id") Integer adminId) {
        validateAdminRole(adminId);
        List<AdminSpecialistDTO> specialists = specialistService.getAllSpecialists()
            .stream()
            .map(this::toAdminSpecialistDTO)
            .toList();
        return ResponseEntity.ok(specialists);
    }
    @DeleteMapping("/specialists/{id}")
    public ResponseEntity<Void> deleteSpecialist(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId) {
        validateAdminRole(adminId);
        boolean deleted = specialistService.deleteSpecialist(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/specialists/{id}/validate/approve")
    public ResponseEntity<Specialist> approveSpecialist(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/specialists/{}/validate/approve - approving specialist", id);
        validateAdminRole(adminId);

        return specialistService.validateSpecialist(id, true)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/specialists/{id}/validate/reject")
    public ResponseEntity<Specialist> rejectSpecialist(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/specialists/{}/validate/reject - rejecting specialist", id);
        validateAdminRole(adminId);

        return specialistService.validateSpecialist(id, false)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId,
            @RequestBody UpdateUserStatusRequest request
    ) {
        logger.info("PUT /api/v1/admin/users/{}/status - updating user status to {}", id, request.getStatus());
        validateAdminRole(adminId);

        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            return ResponseEntity.badRequest().build();
        }

        return userRepository.findById(id)
                .map(user -> {
                    user.setStatus(request.getStatus());
                    user.setLastUpdate(LocalDateTime.now());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/verify")
    public ResponseEntity<VerifyUserResponse> verifyUser(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId,
            @Valid @RequestBody VerifyUserRequest request) {

        logger.info("PUT /api/v1/admin/users/{}/verify - Admin {} verifying user", id, adminId);
        validateAdminRole(adminId);

        VerifyUserResponse response = userVerificationService.verifyUser(id, request);
        logger.info("PUT /api/v1/admin/users/{}/verify - User verified successfully", id);
        return ResponseEntity.ok(response);
    }

    private AdminPatientDTO toAdminPatientDTO(Patient patient) {
        AdminPatientDTO dto = new AdminPatientDTO();
        dto.setId(patient.getId());
        dto.setNames(patient.getNames());
        dto.setFirstLastname(patient.getFirstLastname());
        dto.setSecondLastname(patient.getSecondLastname());
        dto.setEmail(patient.getEmail());
        dto.setBirthDate(patient.getBirthDate());
        dto.setCiNumber(patient.getCiNumber());
        dto.setCiDocumentImg(patient.getCiDocumentImg());
        dto.setSelfieVerification(patient.getSelfieVerification());
        dto.setStatus(patient.getStatus());
        dto.setStrikes(patient.getStrikes());
        dto.setAccountVerified(patient.getAccountVerified());
        dto.setRole(patient.getRole());
        return dto;
    }

    private AdminSpecialistDTO toAdminSpecialistDTO(Specialist specialist) {
        AdminSpecialistDTO dto = new AdminSpecialistDTO();
        dto.setId(specialist.getId());
        dto.setNames(specialist.getNames());
        dto.setFirstLastname(specialist.getFirstLastname());
        dto.setSecondLastname(specialist.getSecondLastname());
        dto.setEmail(specialist.getEmail());
        dto.setBirthDate(specialist.getBirthDate());
        dto.setCiNumber(specialist.getCiNumber());
        dto.setCiDocumentImg(specialist.getCiDocumentImg());
        dto.setCertificationImg(specialist.getCertificationImg());
        dto.setStatus(specialist.getStatus());
        dto.setStrikes(specialist.getStrikes());
        dto.setAccountVerified(specialist.getAccountVerified());
        dto.setRole(specialist.getRole());
        return dto;
    }
}