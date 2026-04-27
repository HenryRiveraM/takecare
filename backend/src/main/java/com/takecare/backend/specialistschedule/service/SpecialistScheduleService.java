package com.takecare.backend.specialistschedule.service;

import java.time.LocalDate;
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

@Service
public class SpecialistScheduleService {

    private static final Byte STATUS_AVAILABLE = 0;
    private static final Byte STATUS_BOOKED = 1;

    @Autowired
    private SpecialistScheduleRepository scheduleRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    public List<SpecialistSchedule> getAvailableSchedules(Integer specialistId) {
        return scheduleRepository.findBySpecialistIdAndStatus(specialistId, STATUS_AVAILABLE);
    }

    @Transactional
    public SpecialistSchedule bookSchedule(Integer id) {
        SpecialistSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        schedule.setStatus(STATUS_BOOKED);

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public SpecialistScheduleResponseDTO createSchedule(Integer specialistId, SpecialistScheduleDTO dto) {

        Specialist specialist = specialistRepository.findById(specialistId)
                .orElseThrow(() -> new RuntimeException("Especialista no encontrado"));

        validateSchedule(dto);

        validateDuplicatedSchedule(specialistId, dto);

        SpecialistSchedule schedule = new SpecialistSchedule();
        schedule.setSpecialist(specialist);
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setStatus(STATUS_AVAILABLE);

        SpecialistSchedule savedSchedule = scheduleRepository.save(schedule);

        return toResponseDTO(savedSchedule);
    }

    public List<SpecialistScheduleResponseDTO> getAllSchedulesBySpecialist(Integer specialistId) {
        return scheduleRepository.findBySpecialistId(specialistId)
                .stream()
                .sorted(Comparator.comparing(SpecialistSchedule::getScheduleDate)
                            .thenComparing(SpecialistSchedule::getStartTime)
                )
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SpecialistScheduleResponseDTO> getSchedulesByDateRange(
            Integer specialistId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return scheduleRepository.findBySpecialistIdAndScheduleDateBetween(
                        specialistId,
                        startDate,
                        endDate
                )
                .stream()
                .sorted(
                        Comparator.comparing(SpecialistSchedule::getScheduleDate)
                                .thenComparing(SpecialistSchedule::getStartTime)
                )
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SpecialistScheduleGroupDTO> getSchedulesGroupedByDay(Integer specialistId) {

        List<SpecialistSchedule> schedules = scheduleRepository.findBySpecialistId(specialistId);

        Map<Byte, List<SpecialistScheduleResponseDTO>> groupedSchedules = schedules.stream()
                .sorted(Comparator.comparing(SpecialistSchedule::getScheduleDate)
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
    public SpecialistScheduleResponseDTO updateSchedule(Integer scheduleId, SpecialistScheduleDTO dto) {

        SpecialistSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        validateSchedule(dto);

        Integer specialistId = schedule.getSpecialist().getId();

        validateDuplicatedScheduleOnUpdate(specialistId, scheduleId, dto);

        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        SpecialistSchedule updatedSchedule = scheduleRepository.save(schedule);

        return toResponseDTO(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(Integer scheduleId) {

        SpecialistSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        scheduleRepository.delete(schedule);
    }

    private void validateSchedule(SpecialistScheduleDTO dto) {

        if (dto.getScheduleDate() == null) {
            throw new RuntimeException("La fecha del horario es obligatoria");
        }

        if (dto.getDayOfWeek() == null) {
            throw new RuntimeException("El día de la semana es obligatorio");
        }

        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new RuntimeException("La hora de inicio y fin son obligatorias");
        }

        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("La hora de inicio debe ser menor que la hora de fin");
        }

        Byte calculatedDayOfWeek = calculateDayOfWeek(dto.getScheduleDate());

        if (!calculatedDayOfWeek.equals(dto.getDayOfWeek())) {
            throw new RuntimeException("El día de la semana no coincide con la fecha seleccionada");
        }
    }

    private void validateDuplicatedSchedule(Integer specialistId, SpecialistScheduleDTO dto) {

        boolean exists = scheduleRepository.existsBySpecialistIdAndScheduleDateAndStartTimeAndEndTime(
                specialistId,
                dto.getScheduleDate(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        if (exists) {
            throw new RuntimeException("Este horario ya existe para esta fecha");
        }
    }

    private void validateDuplicatedScheduleOnUpdate(
            Integer specialistId,
            Integer scheduleId,
            SpecialistScheduleDTO dto
    ) {

    boolean exists = scheduleRepository.existsBySpecialistIdAndScheduleDateAndStartTimeAndEndTimeAndIdNot(
                specialistId,
                dto.getScheduleDate(),
                dto.getStartTime(),
                dto.getEndTime(),
                scheduleId
        );

        if (exists) {
            throw new RuntimeException("Ya existe otro horario igual para esta fecha");
        }
    }

    private Byte calculateDayOfWeek(LocalDate date) {
        return (byte) date.getDayOfWeek().getValue();
    }

    private SpecialistScheduleResponseDTO toResponseDTO(SpecialistSchedule schedule) {
        return new SpecialistScheduleResponseDTO(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getStatus()
        );
    }
}