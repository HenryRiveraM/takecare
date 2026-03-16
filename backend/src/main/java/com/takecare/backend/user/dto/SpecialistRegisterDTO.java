package com.takecare.backend.user.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistRegisterDTO extends UserRegisterDTO {
    
    @NotBlank
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "Specialty must contain only letters")
    private String biography;

    @NotBlank
    private String certificationImg;

    @NotBlank
    private String officeUbi;

    @NotNull
    @Positive
    private BigDecimal sessionCost;
}
