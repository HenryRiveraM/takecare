package com.takecare.backend.session.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSessionHistoryItemDTO {

    private Integer id;
    private Integer patientId;
    private Integer specialistId;
    private Integer scheduleId;

    private String patientName;
    private String specialistName;
    private String patientEmail;
    private String specialistEmail;

    private Integer status;
    private Integer typeOfSession;
    private LocalDateTime createdDate;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
