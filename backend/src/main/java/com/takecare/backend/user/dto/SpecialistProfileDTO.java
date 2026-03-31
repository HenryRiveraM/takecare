package com.takecare.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistProfileDTO {

    private Integer id;
    private String fullName;
    private String email;
    private String biography;
}
