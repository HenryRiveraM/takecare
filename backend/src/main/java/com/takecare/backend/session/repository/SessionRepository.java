package com.takecare.backend.session.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

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
            where sp.id = :specialistId
              and p.id = :patientId
            order by s.createdDate desc
            """)
    List<Session> findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(
            @Param("specialistId") Integer specialistId,
            @Param("patientId") Integer patientId
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

        @Query("""
        select s
        from Session s
        join fetch s.schedule sc
        join fetch sc.specialist sp
        join fetch s.patient p
        where s.status = :status
        """)
    List<Session> findByStatus(@Param("status") Integer status);

    @Query("""
        select count(s) > 0
        from Session s
        join s.schedule sc
        join sc.specialist sp
        join s.patient p
        where sp.id = :specialistId
          and p.id = :patientId
          and s.status in :statuses
        """)
    boolean existsRelationshipBySpecialistAndPatientAndStatuses(
            @Param("specialistId") Integer specialistId,
            @Param("patientId") Integer patientId,
            @Param("statuses") List<Integer> statuses
    );

    @Query("""
        select count(s) > 0
        from Session s
        join s.schedule sc
        join sc.specialist sp
        where sp.id = :specialistId
          and sc.scheduleDate = :scheduleDate
          and s.status in :statuses
          and sc.startTime < :endTime
          and sc.endTime > :startTime
        """)
    boolean existsOverlappingSessionForSpecialist(
            @Param("specialistId") Integer specialistId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<Integer> statuses
    );

    @Query("""
        select count(s) > 0
        from Session s
        join s.schedule sc
        join sc.specialist sp
        where sp.id = :specialistId
          and sc.scheduleDate = :scheduleDate
          and s.status in :statuses
          and s.id <> :excludedSessionId
          and sc.startTime < :endTime
          and sc.endTime > :startTime
        """)
    boolean existsOverlappingSessionForSpecialistExcludingSession(
            @Param("specialistId") Integer specialistId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<Integer> statuses,
            @Param("excludedSessionId") Integer excludedSessionId
    );
        
        @Query("""
        select s
        from Session s
        join fetch s.schedule sc
        join fetch sc.specialist sp
        join fetch s.patient p
        where s.id = :sessionId
        """)
        Optional<Session> findByIdWithDetails(@Param("sessionId") Integer sessionId);

    @Query("""
            select s
            from Session s
            join fetch s.schedule sc
            join fetch sc.specialist sp
            join fetch s.patient p
            where (:status is null or s.status = :status)
              and (:fromDate is null or sc.scheduleDate >= :fromDate)
              and (:toDate is null or sc.scheduleDate <= :toDate)
            order by sc.scheduleDate desc, sc.startTime desc, s.createdDate desc
            """)
    List<Session> findForAdminHistory(
            @Param("status") Integer status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
