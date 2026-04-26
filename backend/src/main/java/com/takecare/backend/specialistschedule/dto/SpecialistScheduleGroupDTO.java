package com.takecare.backend.specialistschedule.dto;

import java.time.DayOfWeek;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SpecialistScheduleGroupDTO {

    private Byte dayOfWeek;
    private List<SpecialistScheduleResponseDTO> schedules;
}