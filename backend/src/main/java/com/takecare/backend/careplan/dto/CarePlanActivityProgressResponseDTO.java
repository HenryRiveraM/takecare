package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CarePlanActivityProgressResponseDTO {

    private Long activityId;
    private String status;
    private LocalDateTime completedDate;
    private Integer planProgressPercentage;
}
