package com.takecare.backend.session.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionRequestDTO {
    private Integer patientId;
    private Integer scheduleId;
    private Integer typeOfSession;
}
