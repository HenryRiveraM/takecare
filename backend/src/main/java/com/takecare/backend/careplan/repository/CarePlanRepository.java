package com.takecare.backend.careplan.repository;

import com.takecare.backend.careplan.model.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {
    List<CarePlan> findBySpecialistIdOrderByCreatedDateDesc(Integer specialistId);
    List<CarePlan> findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(Integer specialistId, Integer patientId);
    List<CarePlan> findByPatientIdOrderByCreatedDateDesc(Integer patientId);
    boolean existsBySpecialistIdAndPatientId(Integer specialistId, Integer patientId);
}
