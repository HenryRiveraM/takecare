package com.takecare.backend.calification.dto;

import java.util.Map;

/**
 * DTO de resumen de calificaciones de un especialista.
 */
public record RatingSummaryDTO(
        Integer specialistId,
        Double average,
        Long total,
        Map<Integer, Long> distribution
) {}
