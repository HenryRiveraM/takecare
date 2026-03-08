package com.takecare.specialistschedule.repository;

import com.takecare.schedulespecialist.model.SpecialistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface SpecialistScheduleRepository 
        extends JpaRepository<SpecialistSchedule, Long> {

}