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

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients(@RequestHeader("X-Admin-Id") Integer adminId) {
        validateAdminRole(adminId);
        return ResponseEntity.ok(patientService.getAllPatients());
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

    @GetMapping("/specialists")
    public ResponseEntity<List<Specialist>> getAllSpecialists(
            @RequestHeader("X-Admin-Id") Integer adminId) {
        validateAdminRole(adminId);
        return ResponseEntity.ok(specialistService.getAllSpecialists());
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
            @PathVariable Integer id
            // , @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/specialists/{}/validate/approve - approving specialist", id);
        // validateAdminRole(adminId);

        return specialistService.validateSpecialist(id, true)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/specialists/{id}/validate/reject")
    public ResponseEntity<Specialist> rejectSpecialist(
            @PathVariable Integer id
            // , @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/specialists/{}/validate/reject - rejecting specialist", id);
        // validateAdminRole(adminId);

        return specialistService.validateSpecialist(id, false)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<User> suspendUser(
            @PathVariable Integer id
            // , @RequestHeader("X-Admin-Id") Integer adminId
    ) {
        logger.info("PUT /api/v1/admin/users/{}/suspend - suspending user", id);
        // validateAdminRole(adminId);

        return userRepository.findById(id)
                .map(user -> {
                    user.setStatus(0);
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
}
