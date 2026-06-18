package com.takecare.backend.careplan.repository;

import com.takecare.backend.careplan.model.TrackingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingNoteRepository extends JpaRepository<TrackingNote, Long> {

    List<TrackingNote> findByCarePlanIdOrderByCreatedDateAsc(Long carePlanId);
}
