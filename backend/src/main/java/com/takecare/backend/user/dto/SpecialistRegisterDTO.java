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
    @Size(max = 1000, message = "Biography must be at most 1000 characters long")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9.,;:!?()\\s-/#]*$", message = "Biography can only contain letters, numbers, spaces, and basic punctuation")
    private String biography;

    @NotBlank
    private String certificationImg;

    @Nullable
    private String officeUbi;

    @NotNull
    @Positive
    private BigDecimal sessionCost;
}
