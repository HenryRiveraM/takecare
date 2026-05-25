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
 
    private static final String EVALUATOR_ROLE_SPECIALIST = "SPECIALIST";
 
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
            Integer sessionPatientId    = getSessionPatientId(session);
 
            logger.info("Create patient rating. sessionId={}, specialistId={}, patientId={}",
                    sessionId, sessionSpecialistId, sessionPatientId);
 
            validateCalifiesAsPatient(session, requestingUser);
            validateSessionAccepted(session);
            validateSessionFinished(session);
 
            if (calificationRepository.existsBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                    sessionId, sessionPatientId, sessionSpecialistId, EVALUATOR_ROLE_PATIENT
            )) {
                logger.warn("Patient rating already exists. sessionId={}, patientId={}",
                        sessionId, sessionPatientId);
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
                    saved.getId(), sessionId, sessionSpecialistId, sessionPatientId);
 
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
            Integer sessionPatientId    = getSessionPatientId(session);
 
            logger.info("Get patient rating. sessionId={}, specialistId={}, patientId={}",
                    sessionId, sessionSpecialistId, sessionPatientId);
 
            validateSessionParticipant(session, requestingUser);
 
            return calificationRepository
                    .findBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                            sessionId, sessionPatientId, sessionSpecialistId, EVALUATOR_ROLE_PATIENT
                    )
                    .map(this::toResponseDto)
                    .orElseThrow(() -> new NoSuchElementException("Calificación no encontrada"));
 
        } catch (RuntimeException e) {
            logger.warn("Get patient rating failed. sessionId={}, error={}",
                    sessionId, e.getMessage());
            throw e;
        }
    }
 
    @Transactional
    public CalificationResponseDTO createSpecialistRating(
            Integer sessionId,
            CreateCalificationRequestDTO request,
            User requestingUser  
    ) {
        try {
            Session session = getSessionOrThrow(sessionId);
 
            Integer sessionSpecialistId = getSessionSpecialistId(session);
            Integer sessionPatientId    = getSessionPatientId(session);
 
            logger.info("Create specialist rating. sessionId={}, specialistId={}, patientId={}",
                    sessionId, sessionSpecialistId, sessionPatientId);
 
            // El usuario que califica debe ser el ESPECIALISTA de la sesión
            validateCalifiesAsSpecialist(session, requestingUser);
            validateSessionAccepted(session);
            validateSessionFinished(session);
 
            if (calificationRepository.existsBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                    sessionId, sessionPatientId, sessionSpecialistId, EVALUATOR_ROLE_SPECIALIST
            )) {
                logger.warn("Specialist rating already exists. sessionId={}, specialistId={}",
                        sessionId, sessionSpecialistId);
                throw new DuplicateKeyException("El especialista ya calificó esta cita");
            }
 
            Calification calification = new Calification();
            calification.setSession(session);
            calification.setPatient(session.getPatient());
            calification.setSpecialist(session.getSchedule().getSpecialist());
            calification.setRating(request.getRating());
            calification.setComment(request.getComment());
            calification.setCreatedDate(LocalDateTime.now());
            calification.setEvaluatorRole(EVALUATOR_ROLE_SPECIALIST); // especialista es quien evalúa
 
            Calification saved = calificationRepository.save(calification);
 
            logger.info("Specialist rating created. calificationId={}, sessionId={}, specialistId={}, patientId={}",
                    saved.getId(), sessionId, sessionSpecialistId, sessionPatientId);
 
            return toResponseDto(saved);
 
        } catch (RuntimeException e) {
            logger.warn("Create specialist rating failed. sessionId={}, error={}",
                    sessionId, e.getMessage());
            throw e;
        }
    }
 
    @Transactional(readOnly = true)
    public CalificationResponseDTO getSpecialistRating(
            Integer sessionId,
            User requestingUser
    ) {
        try {
            Session session = getSessionOrThrow(sessionId);
 
            Integer sessionSpecialistId = getSessionSpecialistId(session);
            Integer sessionPatientId    = getSessionPatientId(session);
 
            logger.info("Get specialist rating. sessionId={}, specialistId={}, patientId={}",
                    sessionId, sessionSpecialistId, sessionPatientId);
 
            validateSessionParticipant(session, requestingUser);
 
            return calificationRepository
                    .findBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
                            sessionId, sessionPatientId, sessionSpecialistId, EVALUATOR_ROLE_SPECIALIST
                    )
                    .map(this::toResponseDto)
                    .orElseThrow(() -> new NoSuchElementException("Calificación no encontrada"));
 
        } catch (RuntimeException e) {
            logger.warn("Get specialist rating failed. sessionId={}, error={}",
                    sessionId, e.getMessage());
            throw e;
        }
    }
 
    @Transactional(readOnly = true)
    public List<CalificationResponseDTO> getRatingsBySpecialist(Integer specialistId) {
        logger.info("Get ratings for specialist {} (from patients)", specialistId);
        return calificationRepository.findBySpecialistIdFromPatients(specialistId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }
 
    @Transactional(readOnly = true)
    public RatingSummaryDTO getRatingSummary(Integer specialistId) {
        logger.info("Get rating summary for specialist {}", specialistId);
 
        List<Calification> ratings =
                calificationRepository.findAllBySpecialistIdFromPatients(specialistId);
 
        long total = ratings.size();
 
        double average = total == 0 ? 0.0 :
                BigDecimal.valueOf(
                        ratings.stream()
                                .mapToInt(Calification::getRating)
                                .average()
                                .orElse(0.0)
                ).setScale(2, RoundingMode.HALF_UP).doubleValue();
 
        Map<Integer, Long> distribution = ratings.stream()
                .collect(Collectors.groupingBy(Calification::getRating, Collectors.counting()));
 
        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }
 
        logger.info("Rating summary for specialist {}: avg={} total={}", specialistId, average, total);
 
        return new RatingSummaryDTO(specialistId, average, total, distribution);
    }

    @Transactional(readOnly = true)
    public List<CalificationResponseDTO> getRatingsByPatient(Integer patientId) {
        logger.info("Get ratings for patient {} (from specialists)", patientId);
        return calificationRepository.findByPatientIdFromSpecialists(patientId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }
 
    private void updateSpecialistReputation(Integer specialistId) {
        if (specialistId == null) return;
 
        List<Calification> ratings =
                calificationRepository.findAllBySpecialistIdFromPatients(specialistId);
 
        if (ratings.isEmpty()) return;
 
        double avg = ratings.stream()
                .mapToInt(Calification::getRating)
                .average()
                .orElse(0.0);
 
        BigDecimal rounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
 
        specialistRepository.findById(specialistId).ifPresent(specialist -> {
            specialist.setReputationAverage(rounded);
            specialistRepository.save(specialist);
            logger.info("Specialist {} reputation updated to {}", specialistId, rounded);
        });
    }
 
    private Session getSessionOrThrow(Integer sessionId) {
        return sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Cita no encontrada"));
    }
 
    private Integer getSessionSpecialistId(Session session) {
        if (session.getSchedule() == null || session.getSchedule().getSpecialist() == null) {
            return null;
        }
        return session.getSchedule().getSpecialist().getId();
    }
 
    private Integer getSessionPatientId(Session session) {
        if (session.getPatient() == null) return null;
        return session.getPatient().getId();
    }
 
    private void validateCalifiesAsPatient(Session session, User requestingUser) {
        Integer sessionPatientId = getSessionPatientId(session);
 
        if (sessionPatientId == null) {
            logger.warn("Session without patient. sessionId={}", session.getId());
            throw new AccessDeniedException("La cita no tiene paciente asignado");
        }
 
        if (requestingUser != null
                && requestingUser.getId() != null
                && !sessionPatientId.equals(requestingUser.getId())) {
            logger.warn("User {} is not the patient {} of session {}",
                    requestingUser.getId(), sessionPatientId, session.getId());
            throw new AccessDeniedException("No eres el paciente de esta cita");
        }
    }

    private void validateCalifiesAsSpecialist(Session session, User requestingUser) {
        Integer sessionSpecialistId = getSessionSpecialistId(session);
 
        if (sessionSpecialistId == null) {
            logger.warn("Session without specialist. sessionId={}", session.getId());
            throw new AccessDeniedException("La cita no tiene especialista asignado");
        }
 
        if (requestingUser != null
                && requestingUser.getId() != null
                && !sessionSpecialistId.equals(requestingUser.getId())) {
            logger.warn("User {} is not the specialist {} of session {}",
                    requestingUser.getId(), sessionSpecialistId, session.getId());
            throw new AccessDeniedException("No eres el especialista de esta cita");
        }
    }
 
    private void validateSessionParticipant(Session session, User requestingUser) {
        if (requestingUser == null || requestingUser.getId() == null) return;
 
        Integer sessionPatientId    = getSessionPatientId(session);
        Integer sessionSpecialistId = getSessionSpecialistId(session);
        Integer userId = requestingUser.getId();
 
        boolean isPatient    = userId.equals(sessionPatientId);
        boolean isSpecialist = userId.equals(sessionSpecialistId);
 
        if (!isPatient && !isSpecialist) {
            logger.warn("User {} is not a participant of session {}", userId, session.getId());
            throw new AccessDeniedException("No tienes acceso a esta calificación");
        }
    }
 
    private void validateSessionAccepted(Session session) {
        Integer status = session.getStatus();
        if (!SESSION_ACCEPTED.equals(status) && !SESSION_FINISHED.equals(status)) {
            logger.warn("Session not in valid state. sessionId={}, status={}", session.getId(), status);
            throw new IllegalStateException("La cita no está en un estado válido para calificar");
        }
    }
 
    private void validateSessionFinished(Session session) {
        if (session.getSchedule() == null
                || session.getSchedule().getScheduleDate() == null
                || session.getSchedule().getEndTime() == null) {
            logger.warn("Session schedule missing end time. sessionId={}", session.getId());
            throw new IllegalStateException("La cita aún no ha finalizado");
        }
 
        LocalDateTime sessionEnd = LocalDateTime.of(
                session.getSchedule().getScheduleDate(),
                session.getSchedule().getEndTime()
        );
 
        if (LocalDateTime.now().isBefore(sessionEnd)) {
            logger.warn("Session not finished yet. sessionId={}, endAt={}", session.getId(), sessionEnd);
            throw new IllegalStateException("La cita aún no ha finalizado");
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