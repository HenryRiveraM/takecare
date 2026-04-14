package com.takecare.backend.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.user.model.Specialist;

@Repository
public interface SpecialistRepository 
        extends JpaRepository<Specialist, Integer> {

    @Query("""
            SELECT DISTINCT s
            FROM Specialist s
            LEFT JOIN FETCH s.specialities sp
            WHERE s.status = 1
              AND s.accountVerified = 1
              AND s.role = 2
            ORDER BY s.names ASC, s.firstLastname ASC, s.secondLastname ASC
            """)
    List<Specialist> findVisibleSpecialists();

    @Query("""
            SELECT DISTINCT s
            FROM Specialist s
            LEFT JOIN FETCH s.specialities sp
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
            LEFT JOIN FETCH s.specialities sp
            WHERE s.id = :id
              AND s.status = 1
              AND s.accountVerified = 1
              AND s.role = 2
            """)
    Optional<Specialist> findVisibleSpecialistById(@Param("id") Integer id);

}