package com.takecare.backend.calification.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.calification.dto.CalificationResponseDTO;
import com.takecare.backend.calification.dto.CreateCalificationRequestDTO;
import com.takecare.backend.calification.dto.RatingSummaryDTO;
import com.takecare.backend.calification.model.Calification;
import com.takecare.backend.calification.repository.CalificationRepository;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class CalificationService {

    private static final Logger logger = LoggerFactory.getLogger(CalificationService.class);

    private static final Integer SESSION_ACCEPTED = 2;
    private static final Integer SESSION_FINISHED = 4;

    private static final String EVALUATOR_ROLE_PATIENT = "PATIENT";

    private final CalificationRepository calificationRepository;
    private final SessionRepository sessionRepository;
    private final SpecialistRepository specialistRepository;

    public CalificationService(
            CalificationRepository calificationRepository,
            SessionRepository sessionRepository,
            SpecialistRepository specialistRepository
    ) {
        this.calificationRepository = calificationRepository;
        this.sessionRepository = sessionRepository;
        this.specialistRepository = specialistRepository;
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

            logger.info("Create patient rating. sessionId={}, specialistId={}",
                    sessionId, sessionSpecialistId);

            validateSessionOwnership(session, requestingUser);
            validateSessionAccepted(session);
            validateSessionFinished(session);

            Integer patientId = session.getPatient().getId();

            if (calificationRepository.existsBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                    sessionId,
                    patientId,
                    sessionSpecialistId,
                    EVALUATOR_ROLE_PATIENT
            )) {

                logger.warn("Patient rating already exists. sessionId={}, specialistId={}, patientId={}",
                        sessionId, sessionSpecialistId, patientId);

                throw new DuplicateKeyException("El paciente ya calificó esta cita");
            }

            Calification calification = new Calification();

            calification.setSession(session);
            calification.setPatient(session.getPatient());
            calification.setSpecialist(session.getSchedule().getSpecialist());

            calification.setRating(request.getRating());
            calification.setComment(request.getComment());

            calification.setCreatedDate(LocalDateTime.now());

            calification.setEvaluatorRole(EVALUATOR_ROLE_PATIENT);

            Calification saved = calificationRepository.save(calification);

            logger.info("Patient rating created. calificationId={}, sessionId={}, specialistId={}, patientId={}",
                    saved.getId(), sessionId, sessionSpecialistId, patientId);

            updateSpecialistReputation(sessionSpecialistId);

            return toResponseDto(saved);

        } catch (RuntimeException e) {

            logger.warn("Create patient rating failed. sessionId={}, error={}",
                    sessionId, e.getMessage());

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

            logger.info("Get patient rating. sessionId={}, specialistId={}",
                    sessionId, sessionSpecialistId);

            validateSessionOwnership(session, requestingUser);

            Integer patientId = session.getPatient().getId();

            return calificationRepository
                    .findBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                            sessionId,
                            patientId,
                            sessionSpecialistId,
                            EVALUATOR_ROLE_PATIENT
                    )
                    .map(this::toResponseDto)
                    .orElseThrow(() ->
                            new NoSuchElementException("Calificación no encontrada"));

        } catch (RuntimeException e) {

            logger.warn("Get patient rating failed. sessionId={}, error={}",
                    sessionId, e.getMessage());

            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<CalificationResponseDTO> getRatingsBySpecialist(Integer specialistId) {

        logger.info("Get ratings for specialist {}", specialistId);

        return calificationRepository.findBySpecialistId(specialistId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryDTO getRatingSummary(Integer specialistId) {

        logger.info("Get rating summary for specialist {}", specialistId);

        List<Calification> ratings =
                calificationRepository.findAllBySpecialistId(specialistId);

        long total = ratings.size();

        double average = total == 0 ? 0.0 :
                BigDecimal.valueOf(
                        ratings.stream()
                                .mapToInt(Calification::getRating)
                                .average()
                                .orElse(0.0)
                ).setScale(2, RoundingMode.HALF_UP).doubleValue();

        Map<Integer, Long> distribution = ratings.stream()
                .collect(Collectors.groupingBy(
                        Calification::getRating,
                        Collectors.counting()
                ));

        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }

        logger.info("Rating summary for specialist {}: avg={} total={}",
                specialistId, average, total);

        return new RatingSummaryDTO(
                specialistId,
                average,
                total,
                distribution
        );
    }

    private void updateSpecialistReputation(Integer specialistId) {

        if (specialistId == null) return;

        List<Calification> ratings =
                calificationRepository.findAllBySpecialistId(specialistId);

        if (ratings.isEmpty()) return;

        double avg = ratings.stream()
                .mapToInt(Calification::getRating)
                .average()
                .orElse(0.0);

        BigDecimal rounded = BigDecimal.valueOf(avg)
                .setScale(2, RoundingMode.HALF_UP);

        specialistRepository.findById(specialistId).ifPresent(specialist -> {

            specialist.setReputationAverage(rounded);

            specialistRepository.save(specialist);

            logger.info("Specialist {} reputation updated to {}",
                    specialistId, rounded);
        });
    }

    private Session getSessionOrThrow(Integer sessionId) {

        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> {

                    logger.warn("Patient rating - session not found. sessionId={}",
                            sessionId);

                    return new NoSuchElementException("Cita no encontrada");
                });
    }

    private Integer getSessionSpecialistId(Session session) {

        if (session.getSchedule() == null
                || session.getSchedule().getSpecialist() == null) {

            return null;
        }

        return session.getSchedule().getSpecialist().getId();
    }

    private void validateSessionOwnership(Session session, User requestingUser) {

        Integer sessionSpecialistId = getSessionSpecialistId(session);

        if (sessionSpecialistId == null) {

            logger.warn("Patient rating - session without specialist. sessionId={}",
                    session.getId());

            throw new AccessDeniedException(
                    "La cita no pertenece al especialista autenticado"
            );
        }

        if (requestingUser != null
                && requestingUser.getId() != null
                && !sessionSpecialistId.equals(requestingUser.getId())) {

            logger.warn(
                    "Patient rating - session not owned by specialist. sessionId={}, specialistId={}, sessionSpecialistId={}",
                    session.getId(),
                    requestingUser.getId(),
                    sessionSpecialistId
            );

            throw new AccessDeniedException(
                    "La cita no pertenece al especialista autenticado"
            );
        }
    }

    private void validateSessionAccepted(Session session) {

        Integer status = session.getStatus();

        if (!SESSION_ACCEPTED.equals(status)
                && !SESSION_FINISHED.equals(status)) {

            logger.warn(
                    "Patient rating - session not accepted. sessionId={}, status={}",
                    session.getId(),
                    status
            );

            throw new IllegalStateException(
                    "La cita no está en un estado válido para calificar"
            );
        }
    }

    private void validateSessionFinished(Session session) {

        if (session.getSchedule() == null
                || session.getSchedule().getScheduleDate() == null
                || session.getSchedule().getEndTime() == null) {

            logger.warn(
                    "Patient rating - session schedule missing end time. sessionId={}",
                    session.getId()
            );

            throw new IllegalStateException(
                    "La cita aún no ha finalizado"
            );
        }

        LocalDateTime sessionEnd = LocalDateTime.of(
                session.getSchedule().getScheduleDate(),
                session.getSchedule().getEndTime()
        );

        if (LocalDateTime.now().isBefore(sessionEnd)) {

            logger.warn(
                    "Patient rating - session not finished. sessionId={}, endAt={}",
                    session.getId(),
                    sessionEnd
            );

            throw new IllegalStateException(
                    "La cita aún no ha finalizado"
            );
        }
    }

    private CalificationResponseDTO toResponseDto(Calification calification) {

        CalificationResponseDTO dto = new CalificationResponseDTO();

        dto.setId(calification.getId());

        dto.setSessionId(
                calification.getSession() != null
                        ? calification.getSession().getId()
                        : null
        );

        dto.setPatientId(
                calification.getPatient() != null
                        ? calification.getPatient().getId()
                        : null
        );

        dto.setSpecialistId(
                calification.getSpecialist() != null
                        ? calification.getSpecialist().getId()
                        : null
        );

        dto.setRating(calification.getRating());

        dto.setComment(calification.getComment());

        dto.setCreatedDate(calification.getCreatedDate());

        dto.setEvaluatorRole(calification.getEvaluatorRole());

        return dto;
    }
}