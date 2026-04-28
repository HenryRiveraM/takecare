package com.takecare.backend.specialistschedule.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.takecare.backend.specialistschedule.dto.SpecialistScheduleDTO;
import com.takecare.backend.specialistschedule.dto.SpecialistScheduleGroupDTO;
import com.takecare.backend.specialistschedule.dto.SpecialistScheduleResponseDTO;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.service.SpecialistScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schedules")
public class SpecialistScheduleController {

    @Autowired
    private SpecialistScheduleService scheduleService;
    
    @PostMapping("/specialist/{specialistId}/create")
    public ResponseEntity<SpecialistScheduleResponseDTO> createSchedule(
            @PathVariable Integer specialistId,
            @Valid @RequestBody SpecialistScheduleDTO dto
    ) {
        return ResponseEntity.ok(scheduleService.createSchedule(specialistId, dto));
    }

    @GetMapping("/specialist/{specialistId}/all")
    public ResponseEntity<List<SpecialistScheduleResponseDTO>> getAllSchedulesBySpecialist(
            @PathVariable Integer specialistId
    ) {
        return ResponseEntity.ok(scheduleService.getAllSchedulesBySpecialist(specialistId));
    }

    @GetMapping("/specialist/{specialistId}/range")
    public ResponseEntity<List<SpecialistScheduleResponseDTO>> getSchedulesByDateRange(
            @PathVariable Integer specialistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                scheduleService.getSchedulesByDateRange(specialistId, startDate, endDate)
        );
    }

    @GetMapping("/specialist/{specialistId}/grouped")
    public ResponseEntity<List<SpecialistScheduleGroupDTO>> getSchedulesGroupedByDay(
            @PathVariable Integer specialistId
    ) {
        return ResponseEntity.ok(scheduleService.getSchedulesGroupedByDay(specialistId));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<SpecialistScheduleResponseDTO> updateSchedule(
            @PathVariable Integer scheduleId,
            @Valid @RequestBody SpecialistScheduleDTO dto
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, dto));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/specialist/{id}")
    public ResponseEntity<List<SpecialistSchedule>> getBySpecialist(@PathVariable Integer id) {
        return ResponseEntity.ok(scheduleService.getAvailableSchedules(id));
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<SpecialistSchedule> book(@PathVariable Integer id) {
        return ResponseEntity.ok(scheduleService.bookSchedule(id));
    }
}