package com.takecare.backend.preventivealert.repository;

import com.takecare.backend.preventivealert.model.PreventiveAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreventiveAlertRepository extends JpaRepository<PreventiveAlert, Long> {

    @Query("""
            select a
            from PreventiveAlert a
            join fetch a.patient p
            where a.specialist.id = :specialistId
            order by case when a.status = 'OPEN' then 0 else 1 end asc, a.createdDate desc
            """)
    List<PreventiveAlert> findBySpecialistIdOrderByStatusAndCreatedDate(
            @Param("specialistId") Integer specialistId
    );

    boolean existsByPatientIdAndAlertTypeAndStatus(
            Integer patientId,
            String alertType,
            String status
    );

    Optional<PreventiveAlert> findByPatientIdAndAlertTypeAndStatus(
            Integer patientId,
            String alertType,
            String status
    );

    Optional<PreventiveAlert> findByCarePlanItemIdAndStatus(
            Long carePlanItemId,
            String status
    );

    Optional<PreventiveAlert> findByIdAndSpecialistId(
            Long alertId,
            Integer specialistId
    );
}
