package com.takecare.backend.specialistschedule.service;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.specialistschedule.dto.SpecialistScheduleDTO;
import com.takecare.backend.specialistschedule.dto.SpecialistScheduleGroupDTO;
import com.takecare.backend.specialistschedule.dto.SpecialistScheduleResponseDTO;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;


@Service
public class SpecialistScheduleService {

    @Autowired
    private SpecialistScheduleRepository scheduleRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<SpecialistSchedule> getAvailableSchedules(Integer specialistId) {
        return scheduleRepository.findBySpecialistIdAndAvailableTrue(specialistId);
    }

    public List<SpecialistSchedule> getSchedulesBySpecialistId(Integer specialistId) {
        return scheduleRepository.findBySpecialistId(specialistId);
    }

    @Transactional
    public SpecialistScheduleResponseDTO createSchedule(Integer specialistId, SpecialistScheduleDTO dto) {

        Specialist specialist = specialistRepository.findById(specialistId)
                .orElseThrow(() -> new RuntimeException("Especialista no encontrado"));

        validateScheduleTime(dto);

        validateDuplicatedSchedule(specialistId, dto);

        SpecialistSchedule schedule = new SpecialistSchedule();
        schedule.setSpecialist(specialist);
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setAvailable(true);

        SpecialistSchedule savedSchedule = scheduleRepository.save(schedule);

        return toResponseDTO(savedSchedule);
    }

    public List<SpecialistScheduleResponseDTO> getAllSchedulesBySpecialist(Integer specialistId) {
        return scheduleRepository.findBySpecialistId(specialistId)
                .stream()
                .sorted(Comparator.comparing(SpecialistSchedule::getDayOfWeek)
                        .thenComparing(SpecialistSchedule::getStartTime))
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SpecialistScheduleGroupDTO> getSchedulesGroupedByDay(Integer specialistId) {

        List<SpecialistSchedule> schedules = scheduleRepository.findBySpecialistId(specialistId);

        Map<DayOfWeek, List<SpecialistScheduleResponseDTO>> groupedSchedules = schedules.stream()
                .sorted(Comparator.comparing(SpecialistSchedule::getDayOfWeek)
                        .thenComparing(SpecialistSchedule::getStartTime))
                .map(this::toResponseDTO)
                .collect(Collectors.groupingBy(SpecialistScheduleResponseDTO::getDayOfWeek));

        return groupedSchedules.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SpecialistScheduleGroupDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public SpecialistScheduleResponseDTO updateSchedule(Long scheduleId, SpecialistScheduleDTO dto) {

        SpecialistSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        validateScheduleTime(dto);

        Integer specialistId = schedule.getSpecialist().getId();

        validateDuplicatedScheduleOnUpdate(specialistId, scheduleId, dto);

        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        SpecialistSchedule updatedSchedule = scheduleRepository.save(schedule);

        return toResponseDTO(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {

        SpecialistSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        scheduleRepository.delete(schedule);
    }

    private void validateScheduleTime(SpecialistScheduleDTO dto) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("La hora de inicio debe ser menor que la hora de fin");
        }
    }

    private void validateDuplicatedSchedule(Integer specialistId, SpecialistScheduleDTO dto) {

        boolean exists = scheduleRepository.existsBySpecialistIdAndDayOfWeekAndStartTimeAndEndTime(
                specialistId,
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        if (exists) {
            throw new RuntimeException("Este horario ya existe para el especialista");
        }
    }

    private void validateDuplicatedScheduleOnUpdate(
            Integer specialistId,
            Long scheduleId,
            SpecialistScheduleDTO dto
    ) {

        boolean exists = scheduleRepository.existsBySpecialistIdAndDayOfWeekAndStartTimeAndEndTimeAndIdNot(
                specialistId,
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime(),
                scheduleId
        );

        if (exists) {
            throw new RuntimeException("Ya existe otro horario igual para este especialista");
        }
    }

    private SpecialistScheduleResponseDTO toResponseDTO(SpecialistSchedule schedule) {
        return new SpecialistScheduleResponseDTO(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isAvailable()
        );
    }
    
    public SpecialistSchedule bookSchedule(Long scheduleId) {
        SpecialistSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Horario no encontrado"));
        
        if (!schedule.isAvailable()) {
            throw new RuntimeException("El horario ya no está disponible");
        }

        schedule.setAvailable(false);
        SpecialistSchedule saved = scheduleRepository.save(schedule);

        messagingTemplate.convertAndSend("/topic/horarios", "ACTUALIZAR_LISTA");
        
        return saved;
    }
}