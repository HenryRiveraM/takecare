package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CarePlanActivityRequestDTO {

    @NotBlank(message = "El titulo de la actividad es obligatorio")
    @Size(max = 150, message = "El titulo de la actividad no puede exceder 150 caracteres")
    private String title;

    @Size(max = 500, message = "La descripcion de la actividad no puede exceder 500 caracteres")
    private String description;

    private LocalDate dueDate;
}
