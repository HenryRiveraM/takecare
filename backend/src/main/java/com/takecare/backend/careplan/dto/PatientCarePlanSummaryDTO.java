package com.takecare.backend.careplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientCarePlanSummaryDTO {

    private Long id;
    private Integer specialistId;
    private String specialistName;
    private String title;
    private String therapeuticObjectives;
    private String generalRecommendations;
    private String professionalObservations;
    private String status;
    private Integer progressPercentage;
    private LocalDate reviewDate;
    private LocalTime reviewStartTime;
    private LocalTime reviewEndTime;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private java.util.List<CarePlanItemResponseDTO> items;
}
