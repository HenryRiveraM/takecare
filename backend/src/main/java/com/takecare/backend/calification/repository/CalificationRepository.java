package com.takecare.backend.calification.repository;

import com.takecare.backend.calification.model.Calification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalificationRepository extends JpaRepository<Calification, Integer> {

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
          AND c.evaluatorRole = 'PATIENT'
        ORDER BY c.createdDate DESC
    """)
    List<Calification> findBySpecialistIdFromPatients(@Param("specialistId") Integer specialistId);

    @Query("""
        SELECT c FROM Calification c
        WHERE c.specialist.id = :specialistId
          AND c.evaluatorRole = 'PATIENT'
    """)
    List<Calification> findAllBySpecialistIdFromPatients(@Param("specialistId") Integer specialistId);

    @Query("""
        SELECT c FROM Calification c
        WHERE c.patient.id = :patientId
          AND c.evaluatorRole = 'SPECIALIST'
        ORDER BY c.createdDate DESC
    """)
    List<Calification> findByPatientIdFromSpecialists(@Param("patientId") Integer patientId);

    @Query("""
        SELECT c FROM Calification c
        WHERE c.patient.id = :patientId
          AND c.evaluatorRole = 'PATIENT'
          AND c.createdDate >= :from
          AND c.createdDate <= :to
    """)
    List<Calification> findPatientRatingsInRange(
            @Param("patientId") Integer patientId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
