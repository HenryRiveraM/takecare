package com.takecare.backend.user.dto;

import java.time.LocalDateTime;

public record ClinicalDocumentDTO(
        Long id,
        Integer patientId,
        String fileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {}
