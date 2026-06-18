package com.takecare.backend.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.notification.dto.NotificationResponseDto;
import com.takecare.backend.notification.dto.NotificationSocketEventDto;
import com.takecare.backend.notification.model.Notification;
import com.takecare.backend.notification.repository.NotificationRepository;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final Byte STATUS_UNREAD = 0;
    private static final Byte STATUS_READ = 1;

    private static final Byte TYPE_NEW_SESSION = 1;
    private static final Byte TYPE_PATIENT_SESSION_RESPONSE = 3;
    private static final Byte TYPE_CARE_PLAN_CREATED = 4;
    private static final Byte TYPE_ITEM_REMINDER = 5;

    private static final String EVENT_NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    private static final String EVENT_NOTIFICATION_STATUS_UPDATED = "NOTIFICATION_STATUS_UPDATED";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               SimpMessagingTemplate messagingTemplate,
                               SessionRepository sessionRepository) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> listBySpecialist(Integer specialistId) {
        logger.info("Listing notifications for specialist id: {}", specialistId);
        return notificationRepository.findAllBySpecialistIdOrderByCreatedDateDesc(specialistId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadBySpecialist(Integer specialistId) {
        return notificationRepository.countUnreadBySpecialistId(specialistId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> listByPatient(Integer patientId) {
        logger.info("Listing notifications for patient id: {}", patientId);
        return notificationRepository.findAllByPatientIdOrderByCreatedDateDesc(patientId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadByPatient(Integer patientId) {
        return notificationRepository.countUnreadByPatientId(patientId);
    }

    @Transactional
    public NotificationResponseDto createForSession(Session session, String description, Byte type) {
        Notification notification = new Notification();
        notification.setSession(session);
        notification.setDescription(normalizeDescription(description));
        notification.setType(type == null ? TYPE_NEW_SESSION : type);
        notification.setStatus(STATUS_UNREAD);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setReadDate(null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);

        logger.info("Notification created successfully with id: {} for specialist id: {}",
                response.getId(), response.getSpecialistId());

        publishNotificationEvent(response.getSpecialistId(), EVENT_NOTIFICATION_CREATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto createForPatientSession(Session session, String description) {
        if (session == null || session.getPatient() == null) {
            throw new RuntimeException("No se puede notificar una cita sin paciente asociado");
        }

        Notification notification = new Notification();
        notification.setSession(session);
        notification.setDescription(normalizeDescription(description));
        notification.setType(TYPE_PATIENT_SESSION_RESPONSE);
        notification.setStatus(STATUS_UNREAD);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setReadDate(null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);

        logger.info("Patient notification created. notificationId={}, patientId={}, sessionId={}",
                response.getId(), response.getPatientId(), response.getSessionId());

        publishPatientNotificationEvent(response.getPatientId(), EVENT_NOTIFICATION_CREATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto createForCarePlan(Integer patientId, Long carePlanId, String description) {
        Notification notification = new Notification();
        notification.setSession(resolveFallbackSessionForPatient(patientId));
        notification.setCarePlanId(carePlanId);
        notification.setDescription(normalizeDescription(description));
        notification.setType(TYPE_CARE_PLAN_CREATED);
        notification.setStatus(STATUS_UNREAD);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setReadDate(null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);
        response.setPatientId(patientId);

        logger.info("Care plan notification created. notificationId={}, patientId={}, carePlanId={}",
                saved.getId(), patientId, carePlanId);

        publishPatientNotificationEvent(patientId, EVENT_NOTIFICATION_CREATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto createForCarePlan(Session session, Long carePlanId, String description) {
        if (session == null || session.getPatient() == null) {
            throw new RuntimeException("No se puede notificar un plan sin paciente asociado");
        }

        Notification notification = new Notification();
        notification.setSession(session);
        notification.setCarePlanId(carePlanId);
        notification.setDescription(normalizeDescription(description));
        notification.setType(TYPE_CARE_PLAN_CREATED);
        notification.setStatus(STATUS_UNREAD);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setReadDate(null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);

        logger.info("Care plan notification created. notificationId={}, patientId={}, carePlanId={}, sessionId={}",
                saved.getId(), response.getPatientId(), carePlanId, session.getId());

        publishPatientNotificationEvent(response.getPatientId(), EVENT_NOTIFICATION_CREATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto createForSpecialistCarePlan(Session session, Long carePlanId, String description) {
        if (session == null) {
            throw new RuntimeException("No se puede notificar al especialista sin una cita de seguimiento");
        }

        Notification notification = new Notification();
        notification.setSession(session);
        notification.setCarePlanId(carePlanId);
        notification.setDescription(normalizeDescription(description));
        notification.setType(TYPE_CARE_PLAN_CREATED);
        notification.setStatus(STATUS_UNREAD);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setReadDate(null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);

        logger.info("Specialist care plan notification created. notificationId={}, specialistId={}, carePlanId={}, sessionId={}",
                saved.getId(), response.getSpecialistId(), carePlanId, session.getId());

        publishNotificationEvent(response.getSpecialistId(), EVENT_NOTIFICATION_CREATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto createItemReminder(Integer patientId, Long carePlanId, Long itemId, String description) {
        Notification notification = new Notification();
        notification.setSession(resolveFallbackSessionForPatient(patientId));
        notification.setCarePlanId(carePlanId);
        notification.setCarePlanItemId(itemId);
        notification.setDescription(normalizeDescription(description));
        notification.setType(TYPE_ITEM_REMINDER);
        notification.setStatus(STATUS_UNREAD);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setReadDate(null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);
        response.setPatientId(patientId);

        logger.info("Item reminder notification created. notificationId={}, patientId={}, carePlanId={}, itemId={}",
                saved.getId(), patientId, carePlanId, itemId);

        publishPatientNotificationEvent(patientId, EVENT_NOTIFICATION_CREATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto updateReadStatus(Integer specialistId, Integer notificationId, boolean read) {
        Notification notification = notificationRepository.findByIdAndSpecialistId(notificationId, specialistId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada para el especialista"));

        notification.setStatus(read ? STATUS_READ : STATUS_UNREAD);
        notification.setReadDate(read ? LocalDateTime.now() : null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);

        logger.info("Notification id: {} updated to status: {}", notificationId, response.getStatus());

        publishNotificationEvent(specialistId, EVENT_NOTIFICATION_STATUS_UPDATED, response);
        return response;
    }

    @Transactional
    public NotificationResponseDto updatePatientReadStatus(Integer patientId, Integer notificationId, boolean read) {
        Notification notification = notificationRepository.findByIdAndPatientId(notificationId, patientId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada para el paciente"));

        notification.setStatus(read ? STATUS_READ : STATUS_UNREAD);
        notification.setReadDate(read ? LocalDateTime.now() : null);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto response = toResponseDto(saved);

        logger.info("Patient notification status updated. notificationId={}, patientId={}, status={}",
                notificationId, patientId, response.getStatus());

        publishPatientNotificationEvent(patientId, EVENT_NOTIFICATION_STATUS_UPDATED, response);
        return response;
    }

    private void publishNotificationEvent(Integer specialistId, String eventType, NotificationResponseDto notification) {
        if (specialistId == null) {
            logger.warn("Skipping socket publish because specialistId is null");
            return;
        }

        NotificationSocketEventDto event = new NotificationSocketEventDto();
        event.setEventType(eventType);
        event.setNotification(notification);
        event.setUnreadCount(countUnreadBySpecialist(specialistId));

        String topic = "/topic/notifications/specialist/" + specialistId;
        messagingTemplate.convertAndSend(topic, event);
        logger.info("Notification event {} sent to topic {}", eventType, topic);
    }

    private void publishPatientNotificationEvent(Integer patientId, String eventType, NotificationResponseDto notification) {
        if (patientId == null) {
            logger.warn("Skipping patient socket publish because patientId is null");
            return;
        }

        NotificationSocketEventDto event = new NotificationSocketEventDto();
        event.setEventType(eventType);
        event.setNotification(notification);
        event.setUnreadCount(countUnreadByPatient(patientId));

        String topic = "/topic/notifications/patient/" + patientId;

        try {
            messagingTemplate.convertAndSend(topic, event);
            logger.info("Patient notification event {} sent to topic {}", eventType, topic);
        } catch (RuntimeException exception) {
            logger.error("Unexpected error sending patient notification event to topic {}", topic, exception);
            throw exception;
        }
    }

    private NotificationResponseDto toResponseDto(Notification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(notification.getId());
        dto.setDescription(notification.getDescription());
        dto.setType(notification.getType());
        dto.setStatus(notification.getStatus());
        dto.setCarePlanId(notification.getCarePlanId());
        dto.setCarePlanItemId(notification.getCarePlanItemId());
        dto.setCreatedDate(notification.getCreatedDate());
        dto.setReadDate(notification.getReadDate());

        if (notification.getSession() != null) {
            dto.setSessionId(notification.getSession().getId());

            if (notification.getSession().getPatient() != null) {
                dto.setPatientId(notification.getSession().getPatient().getId());
            }

            if (notification.getSession().getSchedule() != null
                    && notification.getSession().getSchedule().getSpecialist() != null) {
                dto.setSpecialistId(notification.getSession().getSchedule().getSpecialist().getId());
            }
        }

        return dto;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Nueva notificacion";
        }
        String normalized = description.trim().replaceAll("\\s+", " ");
        return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
    }

    private Session resolveFallbackSessionForPatient(Integer patientId) {
        if (patientId == null) {
            return null;
        }
        List<Session> sessions = sessionRepository.findByPatientIdOrderByCreatedDateDesc(patientId);
        if (!sessions.isEmpty()) {
            return sessions.get(0);
        }
        List<Session> allSessions = sessionRepository.findAll();
        if (!allSessions.isEmpty()) {
            return allSessions.get(0);
        }
        return null;
    }
}
