package com.takecare.backend.specialistschedule.repository;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface SpecialistScheduleRepository extends JpaRepository<SpecialistSchedule, Long> {

    List<SpecialistSchedule> findBySpecialistIdAndAvailableTrue(Integer specialistId);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndAvailableTrue(
            Integer specialistId,
            DayOfWeek dayOfWeek
    );
}
