package com.takecare.backend.careplan.service;

import com.takecare.backend.careplan.dto.CreateLogbookNoteRequestDTO;
import com.takecare.backend.careplan.dto.LogbookNoteResponseDTO;
import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.TrackingNote;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.careplan.repository.TrackingNoteRepository;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TrackingNoteService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingNoteService.class);

    private final TrackingNoteRepository trackingNoteRepository;
    private final CarePlanRepository carePlanRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;

    public TrackingNoteService(
            TrackingNoteRepository trackingNoteRepository,
            CarePlanRepository carePlanRepository,
            SessionRepository sessionRepository,
            NotificationService notificationService
    ) {
        this.trackingNoteRepository = trackingNoteRepository;
        this.carePlanRepository = carePlanRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public LogbookNoteResponseDTO addNote(Long planId, CreateLogbookNoteRequestDTO request) {
        logger.info("Adding tracking note for care plan planId={}, authorRole={}, authorId={}",
                planId, request.getAuthorRole(), request.getAuthorId());

        CarePlan plan = carePlanRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan de cuidado no encontrado"));

        TrackingNote note = new TrackingNote();
        note.setCarePlan(plan);
        note.setAuthorId(request.getAuthorId());
        note.setAuthorRole(request.getAuthorRole().toUpperCase());
        note.setAuthorName(request.getAuthorName().trim());
        note.setNote(request.getContent().trim());
        note.setCreatedDate(LocalDateTime.now());

        // Determine noteType based on role
        String role = request.getAuthorRole().toUpperCase();
        if ("SPECIALIST".equals(role)) {
            note.setNoteType("OBSERVATION");
        } else {
            note.setNoteType("FOLLOW_UP");
        }

        if (request.getSessionId() != null) {
            Session session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new NoSuchElementException("Sesión no encontrada"));
            note.setSession(session);
        }

        TrackingNote saved = trackingNoteRepository.save(note);
        logger.info("Tracking note created with id={}", saved.getId());

        // Notify other user
        try {
            if ("SPECIALIST".equals(role)) {
                if (plan.getPatient() != null) {
                    notificationService.createForCarePlan(
                            plan.getPatient().getId(),
                            plan.getId(),
                            "Nueva nota de tu especialista en la bitácora de: " + plan.getTitle()
                    );
                }
            } else if ("PATIENT".equals(role)) {
                Session notifySession = plan.getReviewSession();
                if (notifySession == null && plan.getPatient() != null && plan.getSpecialist() != null) {
                    List<Session> sessions = sessionRepository.findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(
                            plan.getSpecialist().getId(),
                            plan.getPatient().getId()
                    );
                    if (!sessions.isEmpty()) {
                        notifySession = sessions.get(0);
                    }
                }

                if (notifySession == null && plan.getSpecialist() != null) {
                    List<Session> specialistSessions = sessionRepository.findBySpecialistIdOrderByCreatedDateDesc(
                            plan.getSpecialist().getId()
                    );
                    if (!specialistSessions.isEmpty()) {
                        notifySession = specialistSessions.get(0);
                    }
                }

                if (notifySession != null) {
                    String patientName = plan.getPatient() != null ? plan.getPatient().getNames() : "El paciente";
                    notificationService.createForSpecialistCarePlan(
                            notifySession,
                            plan.getId(),
                            "Nuevo comentario de " + patientName + " en la bitácora de: " + plan.getTitle()
                    );
                } else {
                    logger.warn("Could not send specialist notification because no session could be resolved for specialistId={}",
                            plan.getSpecialist() != null ? plan.getSpecialist().getId() : null);
                }
            }
        } catch (Exception e) {
            logger.error("Error generating notification for tracking note", e);
        }

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LogbookNoteResponseDTO> getNotesByPlan(Long planId) {
        logger.info("Fetching tracking notes for care plan planId={}", planId);

        if (!carePlanRepository.existsById(planId)) {
            throw new NoSuchElementException("Plan de cuidado no encontrado");
        }

        return trackingNoteRepository.findByCarePlanIdOrderByCreatedDateAsc(planId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private LogbookNoteResponseDTO toResponseDTO(TrackingNote note) {
        LogbookNoteResponseDTO dto = new LogbookNoteResponseDTO();
        dto.setId(note.getId());
        dto.setPlanId(note.getCarePlan() != null ? note.getCarePlan().getId() : null);
        dto.setAuthorId(note.getAuthorId());
        dto.setAuthorName(note.getAuthorName());
        dto.setAuthorRole(note.getAuthorRole());
        dto.setContent(note.getNote());
        dto.setCreatedDate(note.getCreatedDate());
        return dto;
    }
}
