package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CarePlanItemRequestDTO {

    @NotBlank(message = "El titulo de la actividad es obligatorio")
    @Size(max = 150, message = "El titulo de la actividad no puede exceder 150 caracteres")
    private String title;

    private String description;

    @NotBlank(message = "El tipo de item es obligatorio")
    private String itemType;

    private LocalDate dueDate;
}
