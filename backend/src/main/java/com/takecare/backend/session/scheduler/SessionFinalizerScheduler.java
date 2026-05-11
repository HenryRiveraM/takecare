package com.takecare.backend.session.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;

@Component
public class SessionFinalizerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SessionFinalizerScheduler.class);

    private static final Integer STATUS_ACCEPTED  = 2;
    private static final Integer STATUS_FINISHED  = 4;

    private final SessionRepository sessionRepository;

    public SessionFinalizerScheduler(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Corre cada 5 minutos.
     * Busca todas las sesiones aceptadas (status=2) cuyo horario ya terminó
     * y las marca como finalizadas (status=4).
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5 minutos en ms
    @Transactional
    public void finalizePastSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<Session> acceptedSessions = sessionRepository
                .findByStatus(STATUS_ACCEPTED);

        if (acceptedSessions.isEmpty()) {
            return;
        }

        List<Session> toFinalize = acceptedSessions.stream()
                .filter(session -> isSessionOver(session, now))
                .toList();

        if (toFinalize.isEmpty()) {
            return;
        }

        toFinalize.forEach(session -> session.setStatus(STATUS_FINISHED));
        sessionRepository.saveAll(toFinalize);

        logger.info("SessionFinalizer: {} sesion(es) marcadas como finalizadas", toFinalize.size());
        toFinalize.forEach(s -> logger.debug(
                "  → sessionId={} scheduleId={}", s.getId(),
                s.getSchedule() != null ? s.getSchedule().getId() : "null"
        ));
    }

    /**
     * Una sesión está terminada cuando la fecha+hora de fin del horario ya pasó.
     */
    private boolean isSessionOver(Session session, LocalDateTime now) {
        SpecialistSchedule schedule = session.getSchedule();
        if (schedule == null) return false;

        LocalDate date    = schedule.getScheduleDate();
        LocalTime endTime = schedule.getEndTime();
        if (date == null || endTime == null) return false;

        LocalDateTime sessionEnd = LocalDateTime.of(date, endTime);
        return sessionEnd.isBefore(now);
    }
}
