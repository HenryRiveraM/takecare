package com.takecare.backend.specialistschedule.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.service.SpecialistScheduleService;

@RestController
@RequestMapping("/api/v1/schedules")
public class SpecialistScheduleController {

    @Autowired
    private SpecialistScheduleService scheduleService;

    @GetMapping("/specialist/{id}")
    public ResponseEntity<List<SpecialistSchedule>> getBySpecialist(@PathVariable Integer id) {
        return ResponseEntity.ok(scheduleService.getAvailableSchedules(id));
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<SpecialistSchedule> book(@PathVariable Integer id) {
        return ResponseEntity.ok(scheduleService.bookSchedule(id));
    }
}