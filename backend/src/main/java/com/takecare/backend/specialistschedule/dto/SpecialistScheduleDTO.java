package com.takecare.backend.specialistschedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialistScheduleDTO {

    @NotNull(message = "El día de la semana es obligatorio")
    @Min(value = 1, message = "El día de la semana debe estar entre 1 y 7")
    @Max(value = 7, message = "El día de la semana debe estar entre 1 y 7")
    private Byte dayOfWeek;

    @NotNull(message = "La fecha de la cita es obligatoria")
    private LocalDate scheduleDate;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;
}