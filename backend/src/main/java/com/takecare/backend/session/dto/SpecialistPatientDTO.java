package com.takecare.backend.session.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistPatientDTO {

    private Integer patientId;
    private String fullName;
    private String email;
    private LocalDate lastSessionDate;
    private LocalDate nextSessionDate;
}
