package com.takecare.backend.session.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelSessionRequestDTO {

    @NotNull(message = "patientId es obligatorio")
    private Integer patientId;
}