package com.takecare.backend.session.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionResponseDTO {

    private Integer id;
    private Integer patientId;
    private Integer scheduleId;
    private Integer specialistId;

    private Integer status;
    private Integer typeOfSession;
    private LocalDateTime createdDate;

    private String patientName;
    private String specialistName;
}