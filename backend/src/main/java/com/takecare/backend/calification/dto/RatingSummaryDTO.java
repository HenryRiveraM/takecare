package com.takecare.backend.calification.dto;

import java.util.Map;

public record RatingSummaryDTO(
        Integer specialistId,
        Double average,
        Long total,
        Map<Integer, Long> distribution
) {}
