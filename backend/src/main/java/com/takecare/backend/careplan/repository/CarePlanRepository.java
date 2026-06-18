package com.takecare.backend.careplan.repository;

import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {

    List<CarePlan> findBySpecialistIdOrderByCreatedDateDesc(Integer specialistId);
    List<CarePlan> findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(Integer specialistId, Integer patientId);
    List<CarePlan> findByPatientIdOrderByCreatedDateDesc(Integer patientId);
    boolean existsBySpecialistIdAndPatientId(Integer specialistId, Integer patientId);

    Optional<CarePlan> findFirstByPatientIdAndStatusOrderByCreatedDateDesc(Integer patientId, CarePlanStatus status);
    List<CarePlan> findByStatus(CarePlanStatus status);
}
