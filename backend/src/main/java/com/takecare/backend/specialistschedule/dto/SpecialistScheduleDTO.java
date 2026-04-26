package com.takecare.backend.specialistschedule.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistScheduleDTO {

    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;
}