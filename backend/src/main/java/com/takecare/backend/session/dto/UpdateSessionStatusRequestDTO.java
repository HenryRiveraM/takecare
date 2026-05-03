package com.takecare.backend.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
}