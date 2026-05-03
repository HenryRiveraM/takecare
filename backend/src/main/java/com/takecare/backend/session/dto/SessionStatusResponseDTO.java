package com.takecare.backend.session.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionStatusResponseDTO {

    private Integer sessionId;
    private Integer specialistId;
    private Integer patientId;
    private Integer scheduleId;
    private Integer status;   
    private Integer scheduleStatus; 
    private LocalDateTime updatedAt;
    private String notificationDescription;
}
