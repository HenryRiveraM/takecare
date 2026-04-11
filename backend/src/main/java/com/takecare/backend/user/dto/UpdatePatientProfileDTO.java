package com.takecare.backend.user.dto;

import java.time.LocalDate;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePatientProfileDTO {

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

}