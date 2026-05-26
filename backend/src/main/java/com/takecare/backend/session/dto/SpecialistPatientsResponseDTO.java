package com.takecare.backend.session.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistPatientsResponseDTO {

    private int totalPatients;
    private List<SpecialistPatientDTO> patients;
}
