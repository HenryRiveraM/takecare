package com.takecare.backend.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistDetailResponseDto {

    private Integer id;
    private String fullName;
    private String email;
    private LocalDate birthDate;
    private String biography;
    private String officeUbi;
    private List<String> specialties;
    private BigDecimal reputationAverage;
    private BigDecimal sessionCost;
    private String certificationImg;
}
