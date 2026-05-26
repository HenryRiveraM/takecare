package com.takecare.backend.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSessionStatusRequestDTO {

    @NotNull(message = "specialistId es obligatorio")
    private Integer specialistId;

    @NotNull(message = "action es obligatorio")
    @Pattern(regexp = "^(accept|reject)$", message = "action solo permite los valores: accept, reject")
    private String action;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
}