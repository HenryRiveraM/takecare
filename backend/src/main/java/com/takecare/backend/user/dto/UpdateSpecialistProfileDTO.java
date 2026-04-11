package com.takecare.backend.user.dto;

import java.math.BigDecimal;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSpecialistProfileDTO {

    @Nullable
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
             message = "Names must contain only letters")
    @Size(max = 30, message = "Names must be at most 30 characters long")
    private String names;

    @Nullable
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
             message = "First lastname must contain only letters")
    @Size(max = 30, message = "First lastname must be at most 30 characters long")
    private String firstLastname;

    @Nullable
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
             message = "Second lastname must contain only letters")
    @Size(max = 30, message = "Second lastname must be at most 30 characters long")
    private String secondLastname;

    @Nullable
    private String officeUbi;

    @Nullable
    @Positive
    private BigDecimal sessionCost;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @Nullable
    @Size(max = 500, message = "Biography must be at most 500 characters long")
    private String biography;
}
