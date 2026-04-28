package com.takecare.backend.session.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.notification.service.NotificationService;
import com.takecare.backend.session.dto.AppointmentStatusResponseDto;
import com.takecare.backend.session.dto.CreateSessionRequestDTO;
import com.takecare.backend.session.dto.SessionResponseDTO;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;

//@Service
public class SessionService {

    private static final Integer SESSION_PENDING = 1;
    private static final Integer SESSION_ACCEPTED = 2;
    private static final Integer SESSION_REJECTED = 3;
    private static final Integer SESSION_FINISHED = 4;
    private static final Integer SESSION_CANCELLED = 5;

    private static final Byte SCHEDULE_AVAILABLE = 0;
    private static final Byte SCHEDULE_UNAVAILABLE = 1;

    private static final Byte NOTIFICATION_TYPE_NEW_SESSION = 1;
    private static final Byte NOTIFICATION_TYPE_SESSION_RESPONSE = 2;

    private final SessionRepository sessionRepository;
    private final PatientRepository patientRepository;
    private final SpecialistScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    public SessionService(
            SessionRepository sessionRepository,
            PatientRepository patientRepository,
            SpecialistScheduleRepository scheduleRepository,
            NotificationService notificationService
    ) {
        this.sessionRepository = sessionRepository;
        this.patientRepository = patientRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SessionResponseDTO createSession(CreateSessionRequestDTO request) {

        if (request.getPatientId() == null || request.getScheduleId() == null) {
            throw new RuntimeException("El paciente y el horario son obligatorios");
        }

        validateTypeOfSession(request.getTypeOfSession());

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        SpecialistSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        sessionRepository.findByScheduleId(schedule.getId())
                .ifPresent(existingSession -> {
                    throw new RuntimeException("Este horario ya tiene una cita registrada");
                });

        validateScheduleForSession(schedule);

        Session session = new Session();
        session.setPatient(patient);
        session.setSchedule(schedule);
        session.setStatus(SESSION_PENDING);
        session.setTypeOfSession(request.getTypeOfSession());
        session.setCreatedDate(LocalDateTime.now());

        Session savedSession = sessionRepository.save(session);

        schedule.setStatus(SCHEDULE_UNAVAILABLE);
        scheduleRepository.save(schedule);

        String patientName = buildFullName(
                patient.getNames(),
                patient.getFirstLastname(),
                patient.getSecondLastname()
        );

        notificationService.createForSession(
                savedSession,
                "Nueva cita solicitada por " + patientName,
                NOTIFICATION_TYPE_NEW_SESSION
        );

        return toResponseDto(savedSession);
    }

    @Transactional(readOnly = true)
    public List<SessionResponseDTO> listByPatient(Integer patientId) {
        return sessionRepository.findByPatientIdOrderByCreatedDateDesc(patientId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponseDTO> listBySpecialist(Integer specialistId) {
        return sessionRepository.findBySpecialistIdOrderByCreatedDateDesc(specialistId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public AppointmentStatusResponseDto updateAppointmentStatus(
            Integer sessionId,
            Integer specialistId,
            String action
    ) {
        Session session = sessionRepository.findByIdAndSpecialistId(sessionId, specialistId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Cita no encontrada o no pertenece al especialista indicado"
                ));

        if (!SESSION_PENDING.equals(session.getStatus())) {
            throw new IllegalStateException("Solo se pueden aprobar o rechazar citas en estado pendiente");
        }

        SpecialistSchedule schedule = session.getSchedule();

        boolean isAccepted = "accept".equalsIgnoreCase(action);
        boolean isRejected = "reject".equalsIgnoreCase(action);

        if (!isAccepted && !isRejected) {
            throw new RuntimeException("Acción inválida. Use accept o reject");
        }

        String notificationDescription;

        if (isAccepted) {
            session.setStatus(SESSION_ACCEPTED);
            schedule.setStatus(SCHEDULE_UNAVAILABLE);
            notificationDescription = "Cita aceptada correctamente";
        } else {
            session.setStatus(SESSION_REJECTED);
            schedule.setStatus(SCHEDULE_AVAILABLE);
            notificationDescription = "Cita rechazada correctamente";
        }

        Session saved = sessionRepository.save(session);
        scheduleRepository.save(schedule);

        notificationService.createForSession(
                saved,
                isAccepted
                        ? "La cita fue aceptada por el especialista"
                        : "La cita fue rechazada por el especialista",
                NOTIFICATION_TYPE_SESSION_RESPONSE
        );

        return buildAppointmentStatusResponse(saved, notificationDescription);
    }

    private void validateScheduleForSession(SpecialistSchedule schedule) {

        if (!SCHEDULE_AVAILABLE.equals(schedule.getStatus())) {
            throw new RuntimeException("Este horario no está disponible");
        }

        if (schedule.getScheduleDate() == null) {
            throw new RuntimeException("El horario no tiene fecha configurada");
        }

        if (schedule.getStartTime() == null) {
            throw new RuntimeException("El horario no tiene hora de inicio configurada");
        }

        LocalDateTime scheduleStartDateTime = LocalDateTime.of(
                schedule.getScheduleDate(),
                schedule.getStartTime()
        );

        LocalDateTime minimumAllowedStartDateTime = LocalDateTime.now().plusHours(1);

        if (scheduleStartDateTime.isBefore(minimumAllowedStartDateTime)) {
            throw new RuntimeException("La cita debe solicitarse con al menos una hora de anticipación");
        }
    }

    private void validateTypeOfSession(Integer typeOfSession) {
        if (typeOfSession == null || (typeOfSession != 1 && typeOfSession != 2)) {
            throw new RuntimeException("El tipo de sesión no es válido");
        }
    }

    private SessionResponseDTO toResponseDto(Session session) {
        SessionResponseDTO dto = new SessionResponseDTO();

        dto.setId(session.getId());
        dto.setStatus(session.getStatus());
        dto.setTypeOfSession(session.getTypeOfSession());
        dto.setCreatedDate(session.getCreatedDate());

        if (session.getPatient() != null) {
            Patient patient = session.getPatient();

            dto.setPatientId(patient.getId());
            dto.setPatientName(buildFullName(
                    patient.getNames(),
                    patient.getFirstLastname(),
                    patient.getSecondLastname()
            ));
        }

        if (session.getSchedule() != null) {
            SpecialistSchedule schedule = session.getSchedule();

            dto.setScheduleId(schedule.getId());

            if (schedule.getSpecialist() != null) {
                dto.setSpecialistId(schedule.getSpecialist().getId());
                dto.setSpecialistName(buildFullName(
                        schedule.getSpecialist().getNames(),
                        schedule.getSpecialist().getFirstLastname(),
                        schedule.getSpecialist().getSecondLastname()
                ));
            }
        }

        return dto;
    }

    private AppointmentStatusResponseDto buildAppointmentStatusResponse(
            Session session,
            String notificationDescription
    ) {
        AppointmentStatusResponseDto dto = new AppointmentStatusResponseDto();

        dto.setSessionId(session.getId());
        dto.setPatientId(session.getPatient().getId());
        dto.setScheduleId(session.getSchedule().getId());
        dto.setSpecialistId(session.getSchedule().getSpecialist().getId());

        dto.setStatus(session.getStatus());

        dto.setScheduleStatus(
                session.getSchedule().getStatus() != null
                        ? session.getSchedule().getStatus().intValue()
                        : null
        );

        dto.setUpdatedAt(LocalDateTime.now());
        dto.setNotificationDescription(notificationDescription);

        return dto;
    }

    private String buildFullName(String names, String firstLastname, String secondLastname) {
        StringBuilder fullName = new StringBuilder();

        if (names != null && !names.isBlank()) {
            fullName.append(names.trim());
        }

        if (firstLastname != null && !firstLastname.isBlank()) {
            fullName.append(" ").append(firstLastname.trim());
        }

        if (secondLastname != null && !secondLastname.isBlank()) {
            fullName.append(" ").append(secondLastname.trim());
        }

        return fullName.toString().trim();
    }
}