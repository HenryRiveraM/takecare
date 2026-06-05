package com.takecare.backend.supportmaterial.DTO;

import java.util.List;

public record SupportMaterialListResponseDto(
        int totalDocuments,
        List<SupportMaterialItemDto> materials
) {}
