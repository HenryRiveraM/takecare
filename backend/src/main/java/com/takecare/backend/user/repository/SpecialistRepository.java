package com.takecare.backend.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.user.model.Specialist;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Integer> {

    @Query("""
        SELECT DISTINCT s FROM Specialist s
        JOIN s.specialties sp
        WHERE LOWER(sp.name) = LOWER(:category)
    """)
    List<Specialist> findBySpecialityName(@Param("category") String category);

    @Query("""
        SELECT DISTINCT s FROM Specialist s
        JOIN s.specialties sp
        JOIN com.takecare.backend.specialistschedule.model.SpecialistSchedule sc
          ON sc.specialist.id = s.id
        WHERE sc.status = 0
          AND (sc.activo = 1 OR sc.activo IS NULL)
          AND sc.dayOfWeek = :dayOfWeek
    """)
    List<Specialist> findByAvailability(@Param("dayOfWeek") Byte dayOfWeek);

    @Query("""
        SELECT DISTINCT s FROM Specialist s
        JOIN s.specialties sp
        JOIN com.takecare.backend.specialistschedule.model.SpecialistSchedule sc
          ON sc.specialist.id = s.id
        WHERE LOWER(sp.name) = LOWER(:category)
          AND sc.status = 0
          AND (sc.activo = 1 OR sc.activo IS NULL)
          AND sc.dayOfWeek = :dayOfWeek
    """)
    List<Specialist> findBySpecialityNameAndAvailability(
            @Param("category") String category,
            @Param("dayOfWeek") Byte dayOfWeek
    );

        @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        LEFT JOIN s.specialties sp
        LEFT JOIN com.takecare.backend.specialistschedule.model.SpecialistSchedule sc
          ON sc.specialist.id = s.id
        WHERE (:dayOfWeek IS NULL OR (sc.status = 0 AND (sc.activo = 1 OR sc.activo IS NULL) AND sc.dayOfWeek = :dayOfWeek))
          AND (
            :name IS NULL
            OR LOWER(CONCAT(
            COALESCE(s.names, ''), ' ',
            COALESCE(s.firstLastname, ''), ' ',
            COALESCE(s.secondLastname, '')
            )) LIKE LOWER(CONCAT('%', :name, '%'))
          )
          AND (
            :category IS NULL
                OR LOWER(COALESCE(sp.name, '')) LIKE LOWER(CONCAT('%', :category, '%'))
                OR LOWER(COALESCE(s.biography, '')) LIKE LOWER(CONCAT('%', :category, '%'))
          )
          AND (
            :city IS NULL
            OR LOWER(COALESCE(s.officeUbi, '')) LIKE LOWER(CONCAT('%', :city, '%'))
          )
        """)
        List<Specialist> findByFilters(
        @Param("name") String name,
        @Param("category") String category,
        @Param("city") String city,
        @Param("dayOfWeek") Byte dayOfWeek
        );

        @Query("""
            SELECT DISTINCT s
            FROM Specialist s
            LEFT JOIN s.specialties sp
            LEFT JOIN com.takecare.backend.specialistschedule.model.SpecialistSchedule sc
              ON sc.specialist.id = s.id
            WHERE (:dayOfWeek IS NULL OR (sc.status = 0 AND (sc.activo = 1 OR sc.activo IS NULL) AND sc.dayOfWeek = :dayOfWeek))
              AND (
                    LOWER(CONCAT(
                        COALESCE(s.names, ''), ' ',
                        COALESCE(s.firstLastname, ''), ' ',
                        COALESCE(s.secondLastname, '')
                    )) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(sp.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(s.biography, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(s.officeUbi, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        """)
        List<Specialist> findBySearchTerm(
                @Param("search") String search,
                @Param("dayOfWeek") Byte dayOfWeek
        );

    @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        LEFT JOIN FETCH s.specialties sp
        WHERE s.status = 1
          AND s.accountVerified = 1
          AND s.role = 2
        ORDER BY s.names ASC, s.firstLastname ASC, s.secondLastname ASC
    """)
    List<Specialist> findVisibleSpecialists();

    @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        LEFT JOIN FETCH s.specialties sp
        WHERE s.status = 1
          AND s.accountVerified = 1
          AND s.role = 2
          AND (
                LOWER(CONCAT(
                    COALESCE(s.names, ''), ' ',
                    COALESCE(s.firstLastname, ''), ' ',
                    COALESCE(s.secondLastname, '')
                )) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(sp.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(s.officeUbi, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY s.names ASC, s.firstLastname ASC, s.secondLastname ASC
    """)
    List<Specialist> searchVisibleSpecialists(@Param("search") String search);

    @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        LEFT JOIN FETCH s.specialties sp
        WHERE s.id = :id
          AND s.status = 1
          AND s.accountVerified = 1
          AND s.role = 2
    """)
    Optional<Specialist> findVisibleSpecialistById(@Param("id") Integer id);
}