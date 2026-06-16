package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateCarePlanRequestDTO {

    @Size(max = 150, message = "El titulo no puede exceder 150 caracteres")
    private String title;

    private String therapeuticObjectives;

    private String generalRecommendations;

    private String professionalObservations;

    private String status;

    @Min(value = 0, message = "El progreso no puede ser menor a 0")
    @Max(value = 100, message = "El progreso no puede ser mayor a 100")
    private Integer progressPercentage;

    private LocalDate reviewDate;

    private LocalTime reviewStartTime;

    private LocalTime reviewEndTime;

    private Integer reviewScheduleId;
}
