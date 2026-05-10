package com.takecare.backend.supportmaterial.dto;

import java.util.List;

public record SupportMaterialListResponseDto(
        int totalDocuments,
        List<SupportMaterialItemDto> materials
) {}
