package com.takecare.backend.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.report.model.Report;

import jakarta.persistence.LockModeType;

@Repository
public interface ReportRepository
        extends JpaRepository<Report, Integer> {

    boolean existsBySessionIdAndReporterIdAndReportedId(
            Integer sessionId,
            Integer reporterId,
            Integer reportedId
    );

    Optional<Report> findBySessionIdAndReporterId(
            Integer sessionId,
            Integer reporterId
    );

    Optional<Report> findBySessionIdAndReportedId(
            Integer sessionId,
            Integer reportedId
    );
    
    List<Report> findAllByReportedId(Integer reportedId);

    @Query("""
            select r
            from Report r
            join fetch r.reporter
            join fetch r.reported
            join fetch r.session s
            join fetch s.schedule
            order by r.createdDate desc, r.id desc
            """)
    List<Report> findAllForAdmin();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Report r where r.id = :id")
    Optional<Report> findByIdForStatusUpdate(@Param("id") Integer id);
}
