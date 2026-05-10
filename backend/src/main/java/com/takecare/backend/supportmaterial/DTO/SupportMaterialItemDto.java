package com.takecare.backend.supportmaterial.dto;

import java.time.LocalDateTime;

public record SupportMaterialItemDto(
        Long id,
        String title,
        String description,
        String fileName,
        String contentType,
        String fileType,
        Long fileSize,
        String fileUrl,
        String previewUrl,
        String downloadUrl,
        LocalDateTime createdDate,
        Integer specialistId,
        String specialistName
) {}
