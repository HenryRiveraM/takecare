package com.takecare.backend.specialistschedule.repository;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SpecialistScheduleRepository extends JpaRepository<SpecialistSchedule, Integer> {

    List<SpecialistSchedule> findBySpecialistId(Integer specialistId);

    List<SpecialistSchedule> findBySpecialistIdAndStatus(Integer specialistId, Byte status);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeek(Integer specialistId, Byte dayOfWeek);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndStatus(
            Integer specialistId,
            Byte dayOfWeek,
            Byte status
    );

    // Nuevo: buscar horarios por rango de fechas
    List<SpecialistSchedule> findBySpecialistIdAndScheduleDateBetween(
            Integer specialistId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<SpecialistSchedule> findBySpecialistIdAndScheduleDateBetweenAndStatus(
            Integer specialistId,
            LocalDate startDate,
            LocalDate endDate,
            Byte status
    );

    // Nuevo: validar duplicados por fecha exacta
    boolean existsBySpecialistIdAndScheduleDateAndStartTimeAndEndTime(
            Integer specialistId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime
    );

    boolean existsBySpecialistIdAndScheduleDateAndStartTimeAndEndTimeAndIdNot(
            Integer specialistId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer id
    );
}