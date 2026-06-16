package com.takecare.backend.careplan.repository;

import com.takecare.backend.careplan.model.CarePlanItem;
import com.takecare.backend.careplan.model.CarePlanItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CarePlanItemRepository extends JpaRepository<CarePlanItem, Long> {

    List<CarePlanItem> findByCarePlanIdOrderByCreatedDateAsc(Long carePlanId);

    void deleteByCarePlanId(Long carePlanId);

    @Query("""
            select i
            from CarePlanItem i
            join fetch i.carePlan cp
            join fetch cp.patient p
            where i.status = :status
              and i.dueDate >= :from
              and i.dueDate <= :to
            """)
    List<CarePlanItem> findPendingItemsDueInRange(
            @Param("status") CarePlanItemStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
