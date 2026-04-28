package com.takecare.backend.session.service;

import com.takecare.backend.notification.service.NotificationService;
import com.takecare.backend.session.dto.AppointmentStatusResponseDto;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import java.util.Optional;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private static final int SESSION_STATUS_PENDING   = 0;
    private static final int SESSION_STATUS_CONFIRMED = 1;
    private static final int SESSION_STATUS_REJECTED  = 2;

    private static final byte SCHEDULE_STATUS_AVAILABLE = 0;
    private static final byte SCHEDULE_STATUS_OCCUPIED  = 1;

    private static final byte NOTIFICATION_TYPE_SESSION_RESPONSE = 2;

    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;

    public AppointmentService(SessionRepository sessionRepository,
                              NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
    }

    /**
     * @param sessionId
     * @param specialistId 
     * @param action      
     * @return 
     */
    @Transactional
    public AppointmentStatusResponseDto updateAppointmentStatus(Integer sessionId,
                                                                Integer specialistId,
                                                                String action) {

        logger.info("PATCH /appointments/{}/status - specialist={} action={}", sessionId, specialistId, action);

        Optional<Session> debug = sessionRepository.findByIdAndSpecialistId(sessionId, specialistId);
            logger.info("DEBUG findByIdAndSpecialistId({}, {}) -> present={}", sessionId, specialistId, debug.isPresent());
            if (debug.isEmpty()) {
                // Probar buscar solo por ID para descartar problema de mapeo
                sessionRepository.findById(sessionId).ifPresent(s ->
                    logger.info("DEBUG findById encontró session, schedule.specialist.id={}",
                        s.getSchedule().getSpecialist().getId())
                );
            }

        Session session = sessionRepository.findByIdAndSpecialistId(sessionId, specialistId)
                .orElseThrow(() -> {
                    logger.warn("Session id={} not found for specialist id={}", sessionId, specialistId);
                    return new NoSuchElementException(
                            "Cita no encontrada o no pertenece al especialista indicado"
                    );
                });

        // 2. Validar que la cita esté pendiente
        if (session.getStatus() == null || session.getStatus() != SESSION_STATUS_PENDING) {
            logger.warn("Session id={} is not in PENDING state, current status={}", sessionId, session.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden aprobar o rechazar citas en estado pendiente"
            );
        }

        SpecialistSchedule schedule = session.getSchedule();
        boolean isAccepted = "accept".equalsIgnoreCase(action);

        if (isAccepted) {
            session.setStatus(SESSION_STATUS_CONFIRMED);
            logger.info("Session id={} CONFIRMED - schedule id={} remains OCCUPIED", sessionId, schedule.getId());
        } else {
            session.setStatus(SESSION_STATUS_REJECTED);
            schedule.setStatus(SCHEDULE_STATUS_AVAILABLE);
            logger.info("Session id={} REJECTED - schedule id={} released to AVAILABLE", sessionId, schedule.getId());
        }

        Session saved = sessionRepository.save(session);

        // 5. Generar notificación para el paciente
        String patientName = buildPatientFullName(session);
        String notificationDescription = isAccepted
                ? "Tu cita fue aceptada por el especialista"
                : "Tu cita fue rechazada por el especialista";

        notificationService.createForSession(
                saved,
                notificationDescription,
                NOTIFICATION_TYPE_SESSION_RESPONSE
        );

        logger.info("Notification created for session id={} patient={}", sessionId, patientName);

        // 6. Construir y retornar respuesta
        return buildResponse(saved, isAccepted, notificationDescription);
    }

    private AppointmentStatusResponseDto buildResponse(Session session,
                                                        boolean accepted,
                                                        String notificationDescription) {
        AppointmentStatusResponseDto dto = new AppointmentStatusResponseDto();
        dto.setSessionId(session.getId());
        dto.setSpecialistId(session.getSchedule().getSpecialist().getId());
        dto.setPatientId(session.getPatient().getId());
        dto.setScheduleId(session.getSchedule().getId());
        dto.setStatus(accepted ? "CONFIRMED" : "REJECTED");
        dto.setScheduleStatus(session.getSchedule().getStatus() != null
                ? session.getSchedule().getStatus().intValue()
                : null);
        dto.setUpdatedAt(LocalDateTime.now());
        dto.setNotificationDescription(notificationDescription);
        return dto;
    }

    private String buildPatientFullName(Session session) {
        if (session.getPatient() == null) return "unknown";
        String names = session.getPatient().getNames();
        String last  = session.getPatient().getFirstLastname();
        if (names == null && last == null) return "unknown";
        return ((names != null ? names : "") + " " + (last != null ? last : "")).trim();
    }
}
