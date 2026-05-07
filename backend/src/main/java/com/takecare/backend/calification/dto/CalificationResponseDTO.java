package com.takecare.backend.calification.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalificationResponseDTO {

    private Integer id;
    private Integer sessionId;
    private Integer patientId;
    private Integer specialistId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdDate;
    private String evaluatorRole;
}
