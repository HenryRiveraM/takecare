package com.takecare.backend.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAdminReportStatusRequestDTO {

    @NotBlank(message = "status es obligatorio")
    private String status;
}
