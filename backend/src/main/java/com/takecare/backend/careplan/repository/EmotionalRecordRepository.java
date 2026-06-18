package com.takecare.backend.careplan.repository;

import com.takecare.backend.careplan.model.EmotionalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionalRecordRepository extends JpaRepository<EmotionalRecord, Long> {

    List<EmotionalRecord> findByPatientIdOrderByCreatedDateDesc(Integer patientId);
}
