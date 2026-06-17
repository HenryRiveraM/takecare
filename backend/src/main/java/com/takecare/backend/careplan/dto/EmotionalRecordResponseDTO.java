package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EmotionalRecordResponseDTO {
    private Long id;
    private Integer patientId;
    private Long carePlanId;
    private String moodState;
    private Integer moodLevel;
    private Integer anxietyLevel;
    private Integer stressLevel;
    private String notes;
    private LocalDateTime createdDate;
}
