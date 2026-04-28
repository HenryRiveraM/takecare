package com.takecare.backend.session.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.session.model.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    @Query("""
            select s
            from Session s
            join fetch s.schedule sc
            where s.id = :sessionId
              and sc.specialist.id = :specialistId
            """)
    Optional<Session> findByIdAndSpecialistId(
            @Param("sessionId") Integer sessionId,
            @Param("specialistId") Integer specialistId
    );
}
