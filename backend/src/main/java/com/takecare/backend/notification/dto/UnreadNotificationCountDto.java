package com.takecare.backend.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnreadNotificationCountDto {

    private Integer specialistId;
    private Long unreadCount;
}
