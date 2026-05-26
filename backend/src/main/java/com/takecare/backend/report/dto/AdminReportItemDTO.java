package com.takecare.backend.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReportItemDTO {

    private Integer id;
    private Integer reporterId;
    private String reporterName;
    private String reporterRole;
    private Integer reportedId;
    private String reportedName;
    private String reportedRole;
    private Integer sessionId;
    private LocalDate sessionDate;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdDate;
}
