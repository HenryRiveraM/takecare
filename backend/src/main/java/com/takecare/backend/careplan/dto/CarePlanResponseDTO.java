package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class CarePlanResponseDTO {
    private Long id;
    private Integer specialistId;
    private Integer patientId;
    private String patientName;
    private String title;
    private String therapeuticObjectives;
    private String generalRecommendations;
    private String professionalObservations;
    private String status;
    private Integer progressPercentage;
    private Integer reviewSessionId;
    private LocalDate reviewDate;
    private LocalTime reviewStartTime;
    private LocalTime reviewEndTime;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Boolean archivedBySpecialist;
    private LocalDateTime archivedDate;
    private List<CarePlanItemResponseDTO> items;
}
