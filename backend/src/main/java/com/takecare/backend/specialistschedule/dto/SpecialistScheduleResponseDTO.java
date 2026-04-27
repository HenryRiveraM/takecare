package com.takecare.backend.specialistschedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SpecialistScheduleResponseDTO {

    private Integer id;
    private Byte dayOfWeek;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Byte status;
}