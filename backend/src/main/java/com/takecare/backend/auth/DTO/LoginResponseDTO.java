package com.takecare.backend.auth.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponseDTO {

    private Integer id;
    private String names;
    private String email;
    private Byte role;
}
