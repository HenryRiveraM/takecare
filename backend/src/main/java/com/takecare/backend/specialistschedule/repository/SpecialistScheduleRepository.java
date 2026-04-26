package com.takecare.backend.specialistschedule.repository;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    boolean existsBySpecialistIdAndDayOfWeekAndStartTimeAndEndTime(
            Integer specialistId,
            Byte dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );

    boolean existsBySpecialistIdAndDayOfWeekAndStartTimeAndEndTimeAndIdNot(
            Integer specialistId,
            Byte dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer id
    );
}