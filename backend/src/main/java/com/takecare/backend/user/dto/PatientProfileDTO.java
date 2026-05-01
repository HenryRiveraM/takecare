package com.takecare.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientProfileDTO {

    private Integer id;
    private String names;
    private String firstLastname;
    private String secondLastname;
    private String ciNumber;
    private String email;
    private String clinicalHistory;
    private Byte accountVerified;
}
