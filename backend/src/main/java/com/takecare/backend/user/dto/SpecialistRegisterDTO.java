package com.takecare.backend.user.dto;

import java.math.BigDecimal;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistRegisterDTO extends UserRegisterDTO {
    
    @NotBlank
    @Size(max = 1000, message = "Profile must be at most 1000 characters long")
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "Specialty must contain only letters")
    private String biography;

    @NotBlank
    private String certificationImg;

    @Nullable
    private String officeUbi;

    @NotNull
    @Positive
    private BigDecimal sessionCost;
}
