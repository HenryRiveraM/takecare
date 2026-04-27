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

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final Byte STATUS_UNREAD = 0;
    private static final Byte STATUS_READ = 1;
    private static final Byte TYPE_NEW_SESSION = 1;

    private static final String EVENT_NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    private static final String EVENT_NOTIFICATION_STATUS_UPDATED = "NOTIFICATION_STATUS_UPDATED";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
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

    @Transactional
    public NotificationResponseDto createForSession(Session session,
                                                    String description,
                                                    Byte type) {
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
    public NotificationResponseDto updateReadStatus(Integer specialistId,
                                                    Integer notificationId,
                                                    boolean read) {
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

    private void publishNotificationEvent(Integer specialistId,
                                          String eventType,
                                          NotificationResponseDto notification) {
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

    private NotificationResponseDto toResponseDto(Notification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(notification.getId());
        dto.setDescription(notification.getDescription());
        dto.setType(notification.getType());
        dto.setStatus(notification.getStatus());
        dto.setCreatedDate(notification.getCreatedDate());
        dto.setReadDate(notification.getReadDate());

        if (notification.getSession() != null) {
            dto.setSessionId(notification.getSession().getId());

            if (notification.getSession().getSchedule() != null
                    && notification.getSession().getSchedule().getSpecialist() != null) {
                dto.setSpecialistId(notification.getSession().getSchedule().getSpecialist().getId());
            }
        }

        return dto;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Nueva cita agendada";
        }

        String normalized = description.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            return normalized.substring(0, 100);
        }
        return normalized;
    }
}
