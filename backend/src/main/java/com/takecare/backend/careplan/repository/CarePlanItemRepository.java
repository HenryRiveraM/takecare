package com.takecare.backend.careplan.repository;

import com.takecare.backend.careplan.model.CarePlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarePlanItemRepository extends JpaRepository<CarePlanItem, Long> {
    List<CarePlanItem> findByCarePlanIdOrderByCreatedDateAsc(Long carePlanId);
    void deleteByCarePlanId(Long carePlanId);
}
