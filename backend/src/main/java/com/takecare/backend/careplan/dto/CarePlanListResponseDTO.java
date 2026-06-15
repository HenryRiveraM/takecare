package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CarePlanListResponseDTO {
    private Integer totalCarePlans;
    private List<CarePlanSummaryDTO> carePlans;
}
