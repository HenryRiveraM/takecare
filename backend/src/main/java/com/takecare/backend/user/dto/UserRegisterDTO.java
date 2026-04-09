package com.takecare.backend.user.dto;

import java.time.LocalDate;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterDTO {

    @NotBlank(message = "Names cannot be blank")
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "Names must contain only letters")
    @Size(max = 30, message = "Names must be at most 30 characters long")
    private String names;

    @NotBlank(message = "First lastname cannot be blank")
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "First lastname must contain only letters")
    @Size(max = 30, message = "Lastnames must be at most 30 characters long")
    private String firstLastname;

    @Nullable
    @Pattern(
    regexp = "^$|^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
    message = "Second lastname must contain only letters")
    @Size(max = 30, message = "Second lastname must be at most 30 characters long")
    private String secondLastname;

    @NotNull
    private LocalDate birthDate;

    @NotBlank(message = "CI number cannot be blank")
    @Pattern(regexp = "^[0-9]+$", message = "CI number must contain only digits")
    @Size(max = 10, message = "CI number must be at most 10 characters long")
    private String ciNumber;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
    message = "Password must contain at least 8 characters, including letters and numbers")
    @Size(max = 30, message = "Password must be at most 30 characters long")
    private String password;
}