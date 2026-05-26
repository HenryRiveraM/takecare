package com.takecare.backend.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.notification.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("""
            select n
            from Notification n
            join fetch n.session s
            join fetch s.schedule sc
            join fetch sc.specialist sp
            where sp.id = :specialistId
              and n.type <> 3
            order by n.createdDate desc
            """)
    List<Notification> findAllBySpecialistIdOrderByCreatedDateDesc(@Param("specialistId") Integer specialistId);

    @Query("""
            select n
            from Notification n
            join fetch n.session s
            join fetch s.schedule sc
            join fetch sc.specialist sp
            where n.id = :notificationId
              and sp.id = :specialistId
              and n.type <> 3
            """)
    Optional<Notification> findByIdAndSpecialistId(
            @Param("notificationId") Integer notificationId,
            @Param("specialistId") Integer specialistId
    );

    @Query("""
            select count(n)
            from Notification n
            join n.session s
            join s.schedule sc
            join sc.specialist sp
            where sp.id = :specialistId
              and n.type <> 3
              and n.status = 0
            """)
    long countUnreadBySpecialistId(@Param("specialistId") Integer specialistId);

    @Query("""
            select n
            from Notification n
            join fetch n.session s
            join fetch s.patient p
            join fetch s.schedule sc
            join fetch sc.specialist sp
            where p.id = :patientId
              and n.type = 3
            order by n.createdDate desc
            """)
    List<Notification> findAllByPatientIdOrderByCreatedDateDesc(@Param("patientId") Integer patientId);

    @Query("""
            select n
            from Notification n
            join fetch n.session s
            join fetch s.patient p
            join fetch s.schedule sc
            join fetch sc.specialist sp
            where n.id = :notificationId
              and p.id = :patientId
              and n.type = 3
            """)
    Optional<Notification> findByIdAndPatientId(
            @Param("notificationId") Integer notificationId,
            @Param("patientId") Integer patientId
    );

    @Query("""
            select count(n)
            from Notification n
            join n.session s
            join s.patient p
            where p.id = :patientId
              and n.type = 3
              and n.status = 0
            """)
    long countUnreadByPatientId(@Param("patientId") Integer patientId);
}
