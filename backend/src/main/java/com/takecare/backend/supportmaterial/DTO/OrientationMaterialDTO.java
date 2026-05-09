package com.takecare.backend.supportmaterial.DTO;

import java.time.LocalDateTime;

public record OrientationMaterialDTO(
        Long id,
        Integer specialistId,
        String title,
        String description,
        String fileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {}
