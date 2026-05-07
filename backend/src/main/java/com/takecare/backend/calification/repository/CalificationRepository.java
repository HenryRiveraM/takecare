package com.takecare.backend.calification.repository;

import com.takecare.backend.calification.model.Calification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalificationRepository
        extends JpaRepository<Calification, Integer> {

    boolean existsBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
            Integer sessionId,
            Integer patientId,
            Integer specialistId,
            String evaluatorRole
    );

    Optional<Calification> findBySessionIdAndPatientIdAndSpecialistIdAndEvaluatorRole(
            Integer sessionId,
            Integer patientId,
            Integer specialistId,
            String evaluatorRole
    );
}