package com.takecare.backend.specialistschedule.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;

@Repository
public interface SpecialistScheduleRepository extends JpaRepository<SpecialistSchedule, Integer> {

    List<SpecialistSchedule> findBySpecialistIdAndActivo(Integer specialistId, Byte activo);

    List<SpecialistSchedule> findBySpecialistIdAndStatusAndActivo(Integer specialistId, Byte status, Byte activo);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndActivo(Integer specialistId, Byte dayOfWeek, Byte activo);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndStatusAndActivo(
            Integer specialistId,
            Byte dayOfWeek,
            Byte status,
            Byte activo
    );

    List<SpecialistSchedule> findBySpecialistIdAndScheduleDateBetweenAndActivo(
            Integer specialistId,
            LocalDate startDate,
            LocalDate endDate,
            Byte activo
    );

    List<SpecialistSchedule> findBySpecialistIdAndScheduleDateBetweenAndStatusAndActivo(
            Integer specialistId,
            LocalDate startDate,
            LocalDate endDate,
            Byte status,
            Byte activo
    );

    boolean existsBySpecialistIdAndScheduleDateAndStartTimeAndEndTimeAndActivo(
            Integer specialistId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            Byte activo
    );

    boolean existsBySpecialistIdAndScheduleDateAndStartTimeAndEndTimeAndIdNotAndActivo(
            Integer specialistId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer id,
            Byte activo
    );
}