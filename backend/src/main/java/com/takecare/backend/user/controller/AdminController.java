package com.takecare.backend.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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

import java.util.List;


@CrossOrigin(origins = "http://localhost:4200")
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