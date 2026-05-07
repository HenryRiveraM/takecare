package com.takecare.backend.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequestDTO {

    @NotNull(message = "specialistId es obligatorio")
    private Integer specialistId;

    @NotNull(message = "sessionId es obligatorio")
    private Integer sessionId;

    @NotBlank(message = "reason es obligatorio")
    @Size(max = 100, message = "reason no puede exceder 100 caracteres")
    private String reason;

    @Size(max = 500, message = "description no puede exceder 500 caracteres")
    private String description;
}
