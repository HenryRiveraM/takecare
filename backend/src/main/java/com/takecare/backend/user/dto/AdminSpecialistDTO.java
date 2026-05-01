package com.takecare.backend.user.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSpecialistDTO {
    private Integer id;
    private String names;
    private String firstLastname;
    private String secondLastname;
    private String email;
    private LocalDate birthDate;
    private String ciNumber;
    private String ciDocumentImg;
    private String certificationImg;
    private Byte role;
    private Byte status;
    private Byte strikes;
    private Byte accountVerified;
}