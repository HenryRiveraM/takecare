package com.takecare.backend.specialities.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SpecialistFilterResponseDTO {

    private Integer id;
    private String names;
    private String firstLastname;
    private String secondLastname;
    private String email;
    private String biography;
    private String officeUbi;
    private BigDecimal sessionCost;
    private BigDecimal reputationAverage;
    private List<String> specialities;
    private List<ScheduleDTO> availableSchedules;

    @Getter
    @Setter
    @Builder
    public static class ScheduleDTO {
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}