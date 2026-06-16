package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CarePlanActivityListResponseDTO {

    private Integer totalActivities;
    private List<CarePlanItemResponseDTO> activities;
}
