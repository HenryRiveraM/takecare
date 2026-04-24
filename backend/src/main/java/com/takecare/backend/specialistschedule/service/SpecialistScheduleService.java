package com.takecare.backend.specialistschedule.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;

@Service
public class SpecialistScheduleService {

    private static final Byte STATUS_AVAILABLE = 0;
    private static final Byte STATUS_BOOKED = 1;

    @Autowired
    private SpecialistScheduleRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<SpecialistSchedule> getAvailableSchedules(Integer specialistId) {
        return repository.findBySpecialistIdAndStatus(specialistId, STATUS_AVAILABLE);
    }

    public SpecialistSchedule bookSchedule(Integer scheduleId) {
        SpecialistSchedule schedule = repository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        if (!STATUS_AVAILABLE.equals(schedule.getStatus())) {
            throw new RuntimeException("El horario ya no está disponible");
        }

        schedule.setStatus(STATUS_BOOKED);

        SpecialistSchedule saved = repository.save(schedule);

        messagingTemplate.convertAndSend("/topic/horarios", "ACTUALIZAR_LISTA");

        return saved;
    }
}