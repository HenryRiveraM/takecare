package com.takecare.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSpecialistLocationRequestDto {

    @NotBlank(message = "addressLine es obligatorio")
    @Size(max = 120, message = "addressLine no puede superar 120 caracteres")
    private String addressLine;

    @NotBlank(message = "city es obligatorio")
    @Size(max = 60, message = "city no puede superar 60 caracteres")
    private String city;

    @Size(max = 60, message = "neighborhood no puede superar 60 caracteres")
    private String neighborhood;

    @Size(max = 120, message = "reference no puede superar 120 caracteres")
    private String reference;

    @Pattern(
            regexp = "^\\s*(public|private)?\\s*$",
            message = "visibility solo permite valores public o private"
    )
    private String visibility;
}
