package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class CreateCarePlanRequestDTO {

    @NotBlank(message = "El titulo del plan es obligatorio")
    @Size(max = 150, message = "El titulo no puede exceder 150 caracteres")
    private String title;

    @NotBlank(message = "Los objetivos terapeuticos son obligatorios")
    private String therapeuticObjectives;

    @NotBlank(message = "Las recomendaciones generales son obligatorias")
    private String generalRecommendations;

    private String professionalObservations;

    private LocalDate reviewDate;

    private LocalTime reviewStartTime;

    private LocalTime reviewEndTime;

    private List<CarePlanItemRequestDTO> items;
}
