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
            left join fetch n.session s
            left join fetch s.patient sp
            left join fetch s.schedule sc
            left join fetch sc.specialist spec
            where (
                  (n.type = 3 and s.patient.id = :patientId)
               or (n.type in (4, 5) and n.carePlanId is not null
                   and exists (
                       select cp from CarePlan cp
                       where cp.id = n.carePlanId
                         and cp.patient.id = :patientId
                   ))
            )
            order by n.createdDate desc
            """)
    List<Notification> findAllByPatientIdOrderByCreatedDateDesc(@Param("patientId") Integer patientId);

    @Query("""
            select n
            from Notification n
            left join fetch n.session s
            left join fetch s.patient sp
            left join fetch s.schedule sc
            left join fetch sc.specialist spec
            where n.id = :notificationId
              and (
                  (n.type = 3 and s.patient.id = :patientId)
               or (n.type in (4, 5) and n.carePlanId is not null
                   and exists (
                       select cp from CarePlan cp
                       where cp.id = n.carePlanId
                         and cp.patient.id = :patientId
                   ))
              )
            """)
    Optional<Notification> findByIdAndPatientId(
            @Param("notificationId") Integer notificationId,
            @Param("patientId") Integer patientId
    );

    @Query("""
            select count(n)
            from Notification n
            left join n.session s
            where n.status = 0
              and (
                  (n.type = 3 and s.patient.id = :patientId)
               or (n.type in (4, 5) and n.carePlanId is not null
                   and exists (
                       select cp from CarePlan cp
                       where cp.id = n.carePlanId
                         and cp.patient.id = :patientId
                   ))
              )
            """)
    long countUnreadByPatientId(@Param("patientId") Integer patientId);

    @Query("""
            select n
            from Notification n
            where n.type = 5
              and n.carePlanItemId = :itemId
              and n.status = 0
            """)
    List<Notification> findUnreadRemindersByItemId(@Param("itemId") Long itemId);
}
