package com.takecare.backend.user.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.report.dto.AdminReportItemDTO;
import com.takecare.backend.report.dto.UpdateAdminReportStatusRequestDTO;
import com.takecare.backend.report.service.ReportService;
import com.takecare.backend.session.dto.AdminSessionHistoryItemDTO;
import com.takecare.backend.session.service.SessionService;
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
    private final SessionService sessionService;
    private final ReportService reportService;

    public AdminController(PatientService patientService,
                           SpecialistService specialistService,
                           UserRepository userRepository,
                           UserVerificationService userVerificationService,
                           SessionService sessionService,
                           ReportService reportService) {
        this.patientService = patientService;
        this.specialistService = specialistService;
        this.userRepository = userRepository;
        this.userVerificationService = userVerificationService;
        this.sessionService = sessionService;
        this.reportService = reportService;
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

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessionHistory(
            @RequestHeader("X-Admin-Id") Integer adminId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        logger.info("GET /api/v1/admin/sessions - adminId={}, status={}, from={}, to={}",
                adminId, status, from, to);

        try {
            validateAdminRole(adminId);
        } catch (RuntimeException exception) {
            logger.warn("GET /api/v1/admin/sessions - access denied: {}", exception.getMessage());
            return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
        }

        try {
            List<AdminSessionHistoryItemDTO> sessions =
                    sessionService.listAdminHistory(status, from, to);
            logger.info("GET /api/v1/admin/sessions - results={}", sessions.size());
            return ResponseEntity.ok(sessions);
        } catch (IllegalArgumentException exception) {
            logger.warn("GET /api/v1/admin/sessions - invalid filters: {}", exception.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (RuntimeException exception) {
            logger.error("GET /api/v1/admin/sessions - unexpected error", exception);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo consultar el historial de citas"));
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReports(@RequestHeader("X-Admin-Id") Integer adminId) {
        logger.info("GET /api/v1/admin/reports - adminId={}", adminId);

        try {
            validateAdminRole(adminId);
        } catch (RuntimeException exception) {
            logger.warn("GET /api/v1/admin/reports - access denied: {}", exception.getMessage());
            return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
        }

        try {
            List<AdminReportItemDTO> reports = reportService.getAdminReports();
            logger.info("GET /api/v1/admin/reports - results={}", reports.size());
            return ResponseEntity.ok(reports);
        } catch (RuntimeException exception) {
            logger.error("GET /api/v1/admin/reports - unexpected error", exception);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo cargar la gestion de reportes"));
        }
    }

    @PutMapping("/reports/{id}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Id") Integer adminId,
            @Valid @RequestBody UpdateAdminReportStatusRequestDTO request
    ) {
        logger.info("PUT /api/v1/admin/reports/{}/status - adminId={}, status={}",
                id, adminId, request.getStatus());

        try {
            validateAdminRole(adminId);
        } catch (RuntimeException exception) {
            logger.warn("PUT /api/v1/admin/reports/{}/status - access denied: {}",
                    id, exception.getMessage());
            return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
        }

        try {
            return ResponseEntity.ok(reportService.updateAdminReportStatus(id, request));
        } catch (IllegalArgumentException exception) {
            logger.warn("PUT /api/v1/admin/reports/{}/status - invalid data: {}",
                    id, exception.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (NoSuchElementException exception) {
            logger.warn("PUT /api/v1/admin/reports/{}/status - not found: {}",
                    id, exception.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            logger.warn("PUT /api/v1/admin/reports/{}/status - already managed: {}",
                    id, exception.getMessage());
            return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
        } catch (RuntimeException exception) {
            logger.error("PUT /api/v1/admin/reports/{}/status - unexpected error", id, exception);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo actualizar el reporte"));
        }
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
                    user.setStatus(request.getStatus().byteValue());
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
        dto.setStatus(patient.getStatus().byteValue());
        dto.setStrikes(patient.getStrikes().byteValue());
        dto.setAccountVerified(patient.getAccountVerified().byteValue());
        dto.setRole(patient.getRole().byteValue());
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
        dto.setStatus(specialist.getStatus().byteValue());
        dto.setStrikes(specialist.getStrikes().byteValue());
        dto.setAccountVerified(specialist.getAccountVerified().byteValue());
        dto.setRole(specialist.getRole().byteValue());
        return dto;
    }
}
