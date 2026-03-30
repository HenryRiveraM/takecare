package com.takecare.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyUserRequest {

    @NotBlank(message = "CI number cannot be blank")
    private String ciNumber;

    private String ciDocumentImg;
}
