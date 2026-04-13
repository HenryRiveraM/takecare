package com.takecare.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistLocationResponseDto {

    private Integer specialistId;
    private String addressLine;
    private String city;
    private String neighborhood;
    private String reference;
    private String visibility;
    private Boolean visibilityPersisted;
    private String officeUbi;
}
