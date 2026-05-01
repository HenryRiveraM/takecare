package com.takecare.backend.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSocketEventDto {

    private String eventType;
    private Long unreadCount;
    private NotificationResponseDto notification;
}
