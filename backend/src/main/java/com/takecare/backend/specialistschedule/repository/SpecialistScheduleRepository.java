package com.takecare.backend.specialistschedule.repository;

import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialistScheduleRepository extends JpaRepository<SpecialistSchedule, Integer> {

    List<SpecialistSchedule> findBySpecialistIdAndStatus(Integer specialistId, Byte status);

    List<SpecialistSchedule> findBySpecialistIdAndDayOfWeekAndStatus(
            Integer specialistId,
            Byte dayOfWeek,
            Byte status
    );
}