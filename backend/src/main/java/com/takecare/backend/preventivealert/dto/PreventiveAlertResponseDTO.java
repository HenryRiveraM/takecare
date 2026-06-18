package com.takecare.backend.preventivealert.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PreventiveAlertResponseDTO {
    private Long id;
    private Integer patientId;
    private String patientName;
    private String priority; // HIGH, MEDIUM, LOW
    private String title;
    private String message;
    private String alertType;
    private String status; // OPEN, REVIEWED
    private LocalDateTime createdDate;
    private LocalDateTime detectedAt;
    private boolean reviewed;
    private LocalDateTime reviewedAt;
}
