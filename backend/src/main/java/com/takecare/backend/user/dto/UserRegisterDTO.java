package com.takecare.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@Getter
@Setter
public class UserRegisterDTO {

    @NotBlank(message = "Names cannot be blank")
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "Names must contain only letters")
    private String names;

    @NotBlank(message = "First lastname cannot be blank")
    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "First lastname must contain only letters")
    private String firstLastname;

    @Pattern(regexp = "^(?!.*[ ]{2})[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$", message = "Second lastname must contain only letters")
    private String secondLastname;

    @NotBlank(message = "Birth date cannot be blank")
    private LocalDate birthDate;

    @NotBlank(message = "CI number cannot be blank")
    @Pattern(regexp = "^[0-9]+$", message = "CI number must contain only digits")
    private String ciNumber;

    @NotBlank(message = "Email cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}