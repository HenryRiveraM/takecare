package com.takecare.backend.specialistschedule.repository;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SpecialistScheduleRepository extends JpaRepository<SpecialistSchedule, Long> {

    List<SpecialistSchedule> findBySpecialistId(Integer specialistId);

    List<SpecialistSchedule> findBySpecialistIdAndAvailableTrue(Integer specialistId);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeek(Integer specialistId, DayOfWeek dayOfWeek);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndAvailableTrue(Integer specialistId, DayOfWeek dayOfWeek);
    
    boolean existsBySpecialistIdAndDayOfWeekAndStartTimeAndEndTime(
            Integer specialistId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );

    boolean existsBySpecialistIdAndDayOfWeekAndStartTimeAndEndTimeAndIdNot(
            Integer specialistId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long id
    );

}
