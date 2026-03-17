package com.takecare.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientRegisterDTO extends UserRegisterDTO {

    @NotBlank
    private String selfieVerification;

    @NotBlank
    private String clinicalHistory;
}
