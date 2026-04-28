package com.takecare.backend.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.takecare.backend.session.model.Session;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    @Query("""
            select s
            from Session s
            join fetch s.schedule sc
            join fetch sc.specialist sp
            join fetch s.patient p
            where s.id = :sessionId
              and sp.id = :specialistId
            """)
    Optional<Session> findByIdAndSpecialistId(
            @Param("sessionId") Integer sessionId,
            @Param("specialistId") Integer specialistId
    );

    Optional<Session> findByScheduleId(Integer scheduleId);

    List<Session> findByPatientIdOrderByCreatedDateDesc(Integer patientId);

    @Query("""
            select s
            from Session s
            join fetch s.schedule sc
            join fetch sc.specialist sp
            join fetch s.patient p
            where sp.id = :specialistId
            order by s.createdDate desc
            """)
    List<Session> findBySpecialistIdOrderByCreatedDateDesc(
            @Param("specialistId") Integer specialistId
    );

    @Query("""
            select s
            from Session s
            join fetch s.schedule sc
            join fetch sc.specialist sp
            join fetch s.patient p
            where s.id = :sessionId
            and p.id = :patientId
            """)
    Optional<Session> findByIdAndPatientId(
            @Param("sessionId") Integer sessionId,
            @Param("patientId") Integer patientId
 );
}