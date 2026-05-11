package com.takecare.backend.calification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.calification.model.Calification;

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

    @Query("""
        SELECT c FROM Calification c
        WHERE c.specialist.id = :specialistId
        ORDER BY c.createdDate DESC
    """)
    List<Calification> findBySpecialistId(@Param("specialistId") Integer specialistId);

    @Query("""
        SELECT c FROM Calification c
        WHERE c.specialist.id = :specialistId
    """)
    List<Calification> findAllBySpecialistId(@Param("specialistId") Integer specialistId);
}
