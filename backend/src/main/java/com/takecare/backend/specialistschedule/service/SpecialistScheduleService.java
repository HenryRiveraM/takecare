package com.takecare.backend.specialistschedule.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;

@Service
public class SpecialistScheduleService {

    @Autowired
    private SpecialistScheduleRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<SpecialistSchedule> getAvailableSchedules(Integer specialistId) {
        return repository.findBySpecialistIdAndAvailableTrue(specialistId);
    }
    
    public SpecialistSchedule bookSchedule(Long scheduleId) {
        SpecialistSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Horario no encontrado"));
        
        if (!schedule.isAvailable()) {
            throw new RuntimeException("El horario ya no está disponible");
        }

        schedule.setAvailable(false);
        SpecialistSchedule saved = repository.save(schedule);

        messagingTemplate.convertAndSend("/topic/horarios", "ACTUALIZAR_LISTA");
        
        return saved;
    }
}