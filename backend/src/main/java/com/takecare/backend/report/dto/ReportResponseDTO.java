package com.takecare.backend.report.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportResponseDTO {

    private Integer id;
    private Integer sessionId;
    private Integer reporterUserId;
    private Integer reportedUserId;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
