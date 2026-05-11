package com.takecare.backend.report.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.report.dto.CreatePatientReportRequestDTO;
import com.takecare.backend.report.dto.CreateReportRequestDTO;
import com.takecare.backend.report.dto.ReportResponseDTO;
import com.takecare.backend.report.model.Report;
import com.takecare.backend.report.repository.ReportRepository;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.user.model.Patient;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private static final Integer SESSION_ACCEPTED = 2;
    private static final Integer SESSION_FINISHED = 4;

    private static final String STATUS_PENDING = "PENDING";

    private static final List<String> ALLOWED_REASONS = List.of(
            "Falta de respeto",
            "Actitud agresiva",
            "Ausencia injustificada",
            "Lenguaje inapropiado",
            "Otro"
    );

    private static final String DUPLICATE_REPORT_MESSAGE =
            "Reporte ya existe para esta sesion y usuario";

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;

    public ReportService(ReportRepository reportRepository, SessionRepository sessionRepository) {
        this.reportRepository = reportRepository;
        this.sessionRepository = sessionRepository;
    }
    
    @Transactional
    public ReportResponseDTO createReport(CreateReportRequestDTO request) {
        Integer sessionId = request != null ? request.getSessionId() : null;
        Integer specialistId = request != null ? request.getSpecialistId() : null;

        try {
            logger.info("Creating report for sessionId={} by specialistId={}",
                    sessionId, specialistId);

            if (request == null) {
                throw new IllegalArgumentException("El request es obligatorio");
            }
            if (specialistId == null) {
                throw new IllegalArgumentException("specialistId es obligatorio");
            }
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId es obligatorio");
            }

            Session session = sessionRepository.findByIdAndSpecialistId(sessionId, specialistId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Cita no encontrada o no pertenece al especialista indicado"
                    ));

            validateSessionStatus(session);

            Patient reportedUser = session.getPatient();
            if (reportedUser == null || reportedUser.getId() == null) {
                throw new NoSuchElementException("Paciente no encontrado para la cita");
            }

            if (reportRepository.existsBySessionIdAndReporterIdAndReportedId(
                    sessionId, specialistId, reportedUser.getId())) {
                throw new IllegalStateException(DUPLICATE_REPORT_MESSAGE);
            }

            String normalizedReason = normalizeReason(request.getReason());
            validateMaxLength("reason", normalizedReason, 100);

            String normalizedDescription = normalizeDescription(request.getDescription());
            if (normalizedDescription != null) {
                validateMaxLength("description", normalizedDescription, 500);
            }

            SpecialistSchedule schedule = session.getSchedule();
            if (schedule == null || schedule.getSpecialist() == null) {
                throw new NoSuchElementException("Especialista no encontrado para la cita");
            }

            Report report = new Report();
            report.setSession(session);
            report.setReporter(schedule.getSpecialist());
            report.setReported(reportedUser);
            report.setReason(normalizedReason);
            report.setDescription(normalizedDescription);
            report.setStatus(STATUS_PENDING);
            report.setCreatedDate(LocalDateTime.now());
            report.setUpdatedDate(report.getCreatedDate());

            Report saved = reportRepository.save(report);

            logger.info("Report created. reportId={} sessionId={} reporterId={} reportedId={}",
                    saved.getId(), sessionId,
                    saved.getReporter() != null ? saved.getReporter().getId() : null,
                    saved.getReported() != null ? saved.getReported().getId() : null);

            return toResponseDto(saved);

        } catch (RuntimeException e) {
            logger.error("Error creating report for sessionId={}", sessionId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating report for sessionId={}", sessionId, e);
            throw new RuntimeException("Error al registrar reporte");
        }
    }

    @Transactional
    public ReportResponseDTO createPatientReport(CreatePatientReportRequestDTO request) {
        Integer sessionId = request != null ? request.getSessionId() : null;
        Integer patientId = request != null ? request.getPatientId() : null;

        try {
            logger.info("Creating patient report for sessionId={} by patientId={}",
                    sessionId, patientId);

            if (request == null) {
                throw new IllegalArgumentException("El request es obligatorio");
            }
            if (patientId == null) {
                throw new IllegalArgumentException("patientId es obligatorio");
            }
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId es obligatorio");
            }

            Session session = sessionRepository.findByIdAndPatientId(sessionId, patientId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Cita no encontrada o no pertenece al paciente indicado"
                    ));

            validateSessionStatus(session);

            SpecialistSchedule schedule = session.getSchedule();
            if (schedule == null || schedule.getSpecialist() == null) {
                throw new NoSuchElementException("Especialista no encontrado para la cita");
            }

            Integer reportedSpecialistId = schedule.getSpecialist().getId();

            if (reportRepository.existsBySessionIdAndReporterIdAndReportedId(
                    sessionId, patientId, reportedSpecialistId)) {
                throw new IllegalStateException(DUPLICATE_REPORT_MESSAGE);
            }

            String normalizedReason = normalizeReason(request.getReason());
            validateMaxLength("reason", normalizedReason, 100);

            String normalizedDescription = normalizeDescription(request.getDescription());
            if (normalizedDescription != null) {
                validateMaxLength("description", normalizedDescription, 500);
            }

            Patient reporter = session.getPatient();
            if (reporter == null || reporter.getId() == null) {
                throw new NoSuchElementException("Paciente no encontrado para la cita");
            }

            Report report = new Report();
            report.setSession(session);
            report.setReporter(reporter);
            report.setReported(schedule.getSpecialist());
            report.setReason(normalizedReason);
            report.setDescription(normalizedDescription);
            report.setStatus(STATUS_PENDING);
            report.setCreatedDate(LocalDateTime.now());
            report.setUpdatedDate(report.getCreatedDate());

            Report saved = reportRepository.save(report);

            logger.info("Patient report created. reportId={} sessionId={} reporterId={} reportedId={}",
                    saved.getId(), sessionId,
                    saved.getReporter() != null ? saved.getReporter().getId() : null,
                    saved.getReported() != null ? saved.getReported().getId() : null);

            return toResponseDto(saved);

        } catch (RuntimeException e) {
            logger.error("Error creating patient report for sessionId={}", sessionId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating patient report for sessionId={}", sessionId, e);
            throw new RuntimeException("Error al registrar reporte");
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/session/{sessionId}/specialist/{specialistId} (ya existía)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportResponseDTO getReportBySession(Integer sessionId, Integer specialistId) {
        try {
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId es obligatorio");
            }
            if (specialistId == null) {
                throw new IllegalArgumentException("specialistId es obligatorio");
            }

            sessionRepository.findByIdAndSpecialistId(sessionId, specialistId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Cita no encontrada o no pertenece al especialista indicado"
                    ));

            return reportRepository.findBySessionIdAndReporterId(sessionId, specialistId)
                    .map(this::toResponseDto)
                    .orElseThrow(() -> new NoSuchElementException("Reporte no encontrado"));

        } catch (RuntimeException e) {
            logger.error("Error fetching report for sessionId={} specialistId={}",
                    sessionId, specialistId, e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/session/{sessionId}/patient/{patientId}
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportResponseDTO getPatientReportBySession(Integer sessionId, Integer patientId) {
        try {
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId es obligatorio");
            }
            if (patientId == null) {
                throw new IllegalArgumentException("patientId es obligatorio");
            }

            sessionRepository.findByIdAndPatientId(sessionId, patientId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Cita no encontrada o no pertenece al paciente indicado"
                    ));

            // El paciente es el reporter en este caso
            return reportRepository.findBySessionIdAndReporterId(sessionId, patientId)
                    .map(this::toResponseDto)
                    .orElseThrow(() -> new NoSuchElementException("Reporte no encontrado"));

        } catch (RuntimeException e) {
            logger.error("Error fetching patient report for sessionId={} patientId={}",
                    sessionId, patientId, e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/specialists/{specialistId}/reports
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> getReportsReceivedBySpecialist(Integer specialistId) {
        try {
            if (specialistId == null) {
                throw new IllegalArgumentException("specialistId es obligatorio");
            }

            logger.info("Fetching reports received by specialistId={}", specialistId);

            return reportRepository.findAllByReportedId(specialistId)
                    .stream()
                    .map(this::toResponseDto)
                    .collect(Collectors.toList());

        } catch (RuntimeException e) {
            logger.error("Error fetching reports for specialistId={}", specialistId, e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/patients/{patientId}/reports
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> getReportsReceivedByPatient(Integer patientId) {
        try {
            if (patientId == null) {
                throw new IllegalArgumentException("patientId es obligatorio");
            }

            logger.info("Fetching reports received by patientId={}", patientId);

            return reportRepository.findAllByReportedId(patientId)
                    .stream()
                    .map(this::toResponseDto)
                    .collect(Collectors.toList());

        } catch (RuntimeException e) {
            logger.error("Error fetching reports for patientId={}", patientId, e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void validateSessionStatus(Session session) {
        if (session == null || session.getStatus() == null) {
            throw new IllegalStateException("La cita no esta en un estado valido para reportar");
        }

        Integer status = session.getStatus();
        if (!SESSION_ACCEPTED.equals(status) && !SESSION_FINISHED.equals(status)) {
            throw new IllegalStateException("La cita no esta en un estado valido para reportar");
        }
    }

    private String normalizeReason(String reason) {
        String normalized = normalizeText(reason);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("reason es obligatorio");
        }

        for (String allowed : ALLOWED_REASONS) {
            if (allowed.equalsIgnoreCase(normalized)) {
                return allowed;
            }
        }

        throw new IllegalArgumentException(
                "reason no permitido. Valores permitidos: " + String.join(", ", ALLOWED_REASONS)
        );
    }

    private String normalizeDescription(String description) {
        String normalized = normalizeText(description);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " no puede exceder " + maxLength + " caracteres"
            );
        }
    }

    private ReportResponseDTO toResponseDto(Report report) {
        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setId(report.getId());
        dto.setReason(report.getReason());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setCreatedDate(report.getCreatedDate());
        dto.setUpdatedDate(report.getUpdatedDate());

        if (report.getSession() != null) {
            dto.setSessionId(report.getSession().getId());
        }
        if (report.getReporter() != null) {
            dto.setReporterUserId(report.getReporter().getId());
        }
        if (report.getReported() != null) {
            dto.setReportedUserId(report.getReported().getId());
        }

        return dto;
    }

    public static String getDuplicateReportMessage() {
        return DUPLICATE_REPORT_MESSAGE;
    }
}
