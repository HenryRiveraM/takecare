package com.takecare.backend.calification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCalificationRequestDTO {

    @NotNull(message = "rating es obligatorio")
    @Min(value = 1, message = "rating debe ser minimo 1")
    @Max(value = 5, message = "rating debe ser maximo 5")
    private Integer rating;

    @Size(max = 240, message = "comment no puede superar 240 caracteres")
    private String comment;
}
