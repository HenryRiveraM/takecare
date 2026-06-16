package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatientCarePlanListResponseDTO {

    private Integer totalCarePlans;
    private List<PatientCarePlanSummaryDTO> carePlans;
}
