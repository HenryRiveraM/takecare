package com.takecare.backend.session.repository;

import java.util.List;
import java.util.Optional;

import com.takecare.backend.session.model.Session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    Optional<Session> findByScheduleId(Integer scheduleId);

    List<Session> findByPatientIdOrderByCreatedDateDesc(Integer patientId);
}