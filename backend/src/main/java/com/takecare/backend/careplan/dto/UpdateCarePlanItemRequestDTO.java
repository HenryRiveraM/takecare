package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateCarePlanItemRequestDTO {

    @Size(max = 150, message = "El titulo de la actividad no puede exceder 150 caracteres")
    private String title;

    private String description;

    private String itemType;

    private String status;

    private LocalDate dueDate;
}
