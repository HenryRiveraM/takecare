package com.takecare.backend.notification.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.notification.dto.NotificationResponseDto;
import com.takecare.backend.notification.dto.UnreadNotificationCountDto;
import com.takecare.backend.notification.dto.UpdateNotificationStatusRequestDto;
import com.takecare.backend.notification.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/specialist/{specialistId}")
    public ResponseEntity<List<NotificationResponseDto>> listBySpecialist(@PathVariable Integer specialistId) {
        logger.info("GET /api/v1/notifications/specialist/{} - listing notifications", specialistId);
        return ResponseEntity.ok(notificationService.listBySpecialist(specialistId));
    }

    @GetMapping("/specialist/{specialistId}/unread-count")
    public ResponseEntity<UnreadNotificationCountDto> unreadCount(@PathVariable Integer specialistId) {
        logger.info("GET /api/v1/notifications/specialist/{}/unread-count", specialistId);

        UnreadNotificationCountDto response = new UnreadNotificationCountDto();
        response.setSpecialistId(specialistId);
        response.setUnreadCount(notificationService.countUnreadBySpecialist(specialistId));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/read-status")
    public ResponseEntity<NotificationResponseDto> updateReadStatus(
            @PathVariable Integer notificationId,
            @RequestBody UpdateNotificationStatusRequestDto request) {

        logger.info("PUT /api/v1/notifications/{}/read-status", notificationId);

        if (request.getSpecialistId() == null || request.getRead() == null) {
            return ResponseEntity.badRequest().build();
        }

        NotificationResponseDto updated = notificationService.updateReadStatus(
                request.getSpecialistId(),
                notificationId,
                request.getRead()
        );

        return ResponseEntity.ok(updated);
    }
}
