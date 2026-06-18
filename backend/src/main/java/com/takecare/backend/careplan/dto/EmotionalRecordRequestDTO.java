package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmotionalRecordRequestDTO {

    @NotNull(message = "El nivel de animo es obligatorio")
    @Min(value = 1, message = "El nivel de animo debe estar entre 1 y 5")
    @Max(value = 5, message = "El nivel de animo debe estar entre 1 y 5")
    private Integer moodLevel;

    @NotNull(message = "El nivel de ansiedad es obligatorio")
    @Min(value = 1, message = "El nivel de ansiedad debe estar entre 1 y 5")
    @Max(value = 5, message = "El nivel de ansiedad debe estar entre 1 y 5")
    private Integer anxietyLevel;

    @NotNull(message = "El nivel de estres es obligatorio")
    @Min(value = 1, message = "El nivel de estres debe estar entre 1 y 5")
    @Max(value = 5, message = "El nivel de estres debe estar entre 1 y 5")
    private Integer stressLevel;

    @Size(max = 280, message = "Las notas no pueden exceder 280 caracteres")
    private String notes;
}
