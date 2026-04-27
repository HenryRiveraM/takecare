package com.takecare.backend.notification.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponseDto {

    private Integer id;
    private Integer sessionId;
    private Integer specialistId;
    private String description;
    private Byte type;
    private Byte status;
    private LocalDateTime createdDate;
    private LocalDateTime readDate;
}
