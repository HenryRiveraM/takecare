package com.takecare.backend.calification.service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.calification.dto.CalificationResponseDTO;
import com.takecare.backend.calification.dto.CreateCalificationRequestDTO;
import com.takecare.backend.calification.model.Calification;
import com.takecare.backend.calification.repository.CalificationRepository;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.user.model.User;

@Service
public class CalificationService {

    private static final Logger logger = LoggerFactory.getLogger(CalificationService.class);

    private static final Integer SESSION_ACCEPTED = 2;
    private static final String EVALUATOR_ROLE_SPECIALIST = "SPECIALIST";

    private final CalificationRepository calificationRepository;
    private final SessionRepository sessionRepository;

    public CalificationService(
            CalificationRepository calificationRepository,
            SessionRepository sessionRepository
    ) {
        this.calificationRepository = calificationRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public CalificationResponseDTO createPatientRating(
            Integer sessionId,
            CreateCalificationRequestDTO request,
            User requestingUser
    ) {
        try {
            Session session = getSessionOrThrow(sessionId);
            Integer sessionSpecialistId = getSessionSpecialistId(session);

            logger.info("Create patient rating. sessionId={}, specialistId={}", sessionId, sessionSpecialistId);

            validateSessionOwnership(session, requestingUser);
            validateSessionAccepted(session);
            validateSessionFinished(session);

            Integer patientId = session.getPatient().getId();

            if (calificationRepository.existsBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                    sessionId,
                    patientId,
                    sessionSpecialistId,
                    EVALUATOR_ROLE_SPECIALIST
            )) {
                logger.warn("Patient rating already exists. sessionId={}, specialistId={}, patientId={}",
                        sessionId, sessionSpecialistId, patientId);
                throw new DuplicateKeyException("El especialista ya califico esta cita");
            }

            Calification calification = new Calification();
            calification.setSession(session);
            calification.setPatient(session.getPatient());
            calification.setSpecialist(session.getSchedule().getSpecialist());
            calification.setRating(request.getRating());
            calification.setComment(request.getComment());
            calification.setCreatedDate(LocalDateTime.now());
            calification.setEvaluatorRole(EVALUATOR_ROLE_SPECIALIST);

            Calification saved = calificationRepository.save(calification);

            logger.info("Patient rating created. calificationId={}, sessionId={}, specialistId={}, patientId={}",
                    saved.getId(), sessionId, sessionSpecialistId, patientId);

            return toResponseDto(saved);

        } catch (RuntimeException e) {
            logger.warn("Create patient rating failed. sessionId={}, error={}", sessionId, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public CalificationResponseDTO getPatientRating(
            Integer sessionId,
            User requestingUser
    ) {
        try {
            Session session = getSessionOrThrow(sessionId);
            Integer sessionSpecialistId = getSessionSpecialistId(session);

            logger.info("Get patient rating. sessionId={}, specialistId={}", sessionId, sessionSpecialistId);

            validateSessionOwnership(session, requestingUser);

            Integer patientId = session.getPatient().getId();

            return calificationRepository.findBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                    sessionId,
                    patientId,
                    sessionSpecialistId,
                    EVALUATOR_ROLE_SPECIALIST
            ).map(this::toResponseDto)
             .orElseThrow(() -> new NoSuchElementException("Calificacion no encontrada"));

        } catch (RuntimeException e) {
            logger.warn("Get patient rating failed. sessionId={}, error={}", sessionId, e.getMessage());
            throw e;
        }
    }

    private Session getSessionOrThrow(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    logger.warn("Patient rating - session not found. sessionId={}", sessionId);
                    return new NoSuchElementException("Cita no encontrada");
                });
    }

    private Integer getSessionSpecialistId(Session session) {
        if (session.getSchedule() == null || session.getSchedule().getSpecialist() == null) {
            return null;
        }
        return session.getSchedule().getSpecialist().getId();
    }

    private void validateSessionOwnership(Session session, User requestingUser) {
        Integer sessionSpecialistId = getSessionSpecialistId(session);

        if (sessionSpecialistId == null) {
            logger.warn("Patient rating - session without specialist. sessionId={}", session.getId());
            throw new AccessDeniedException("La cita no pertenece al especialista autenticado");
        }

        if (requestingUser != null && requestingUser.getId() != null
                && !sessionSpecialistId.equals(requestingUser.getId())) {
            logger.warn("Patient rating - session not owned by specialist. sessionId={}, specialistId={}, sessionSpecialistId={}",
                    session.getId(), requestingUser.getId(), sessionSpecialistId);
            throw new AccessDeniedException("La cita no pertenece al especialista autenticado");
        }
    }

    private void validateSessionAccepted(Session session) {
        if (!SESSION_ACCEPTED.equals(session.getStatus())) {
            logger.warn("Patient rating - session not accepted. sessionId={}, status={}",
                    session.getId(), session.getStatus());
            throw new IllegalStateException("La cita no esta aceptada");
        }
    }

    private void validateSessionFinished(Session session) {
        if (session.getSchedule() == null
                || session.getSchedule().getScheduleDate() == null
                || session.getSchedule().getEndTime() == null) {
            logger.warn("Patient rating - session schedule missing end time. sessionId={}", session.getId());
            throw new IllegalStateException("La cita aun no ha finalizado");
        }

        LocalDateTime sessionEnd = LocalDateTime.of(
                session.getSchedule().getScheduleDate(),
                session.getSchedule().getEndTime()
        );

        if (LocalDateTime.now().isBefore(sessionEnd)) {
            logger.warn("Patient rating - session not finished. sessionId={}, endAt={}",
                    session.getId(), sessionEnd);
            throw new IllegalStateException("La cita aun no ha finalizado");
        }
    }

    private CalificationResponseDTO toResponseDto(Calification calification) {
        CalificationResponseDTO dto = new CalificationResponseDTO();

        dto.setId(calification.getId());
        dto.setSessionId(calification.getSession() != null ? calification.getSession().getId() : null);
        dto.setPatientId(calification.getPatient() != null ? calification.getPatient().getId() : null);
        dto.setSpecialistId(calification.getSpecialist() != null ? calification.getSpecialist().getId() : null);
        dto.setRating(calification.getRating());
        dto.setComment(calification.getComment());
        dto.setCreatedDate(calification.getCreatedDate());
        dto.setEvaluatorRole(calification.getEvaluatorRole());

        return dto;
    }
}
