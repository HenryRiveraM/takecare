package com.takecare.backend.report.dto;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
 
public class CreatePatientReportRequestDTO {
 
    @NotNull(message = "patientId es obligatorio")
    private Integer patientId;
 
    @NotNull(message = "sessionId es obligatorio")
    private Integer sessionId;
 
    @NotBlank(message = "reason es obligatorio")
    @Size(max = 100, message = "reason no puede exceder 100 caracteres")
    private String reason;
 
    @Size(max = 500, message = "description no puede exceder 500 caracteres")
    private String description;
 
    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }
 
    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }
 
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
 
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}