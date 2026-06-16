package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CarePlanItemResponseDTO {
    private Long id;
    private Long planId;
    private String title;
    private String description;
    private String itemType;
    private String status;
    private LocalDate dueDate;
    private LocalDateTime completedDate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Integer planProgressPercentage;
}
