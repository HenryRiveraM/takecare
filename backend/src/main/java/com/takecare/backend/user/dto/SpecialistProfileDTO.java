package com.takecare.backend.user.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistProfileDTO {

    private Integer id;
    private String names;
    private String firstLastname;
    private String secondLastname;
    private String officeUbi;
    private BigDecimal sessionCost;
    private String email;
    private String biography;
}
