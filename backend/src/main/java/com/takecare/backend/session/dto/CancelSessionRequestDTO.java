package com.takecare.backend.session.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelSessionRequestDTO {

    private Integer patientId;
    private Integer specialistId;
}