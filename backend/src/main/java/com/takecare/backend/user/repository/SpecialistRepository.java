package com.takecare.backend.user.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.user.model.Specialist;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Integer> {

    @Query("""
        SELECT DISTINCT s FROM Specialist s
        JOIN s.specialities sp
        WHERE LOWER(sp.name) = LOWER(:category)
    """)
    List<Specialist> findBySpecialityName(@Param("category") String category);

    @Query("""
        SELECT DISTINCT s FROM Specialist s
        JOIN s.specialities sp
        JOIN com.takecare.backend.specialistschedule.model.SpecialistSchedule sc
          ON sc.specialist.id = s.id
        WHERE sc.available = true
          AND sc.dayOfWeek = :dayOfWeek
    """)
    List<Specialist> findByAvailability(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("""
        SELECT DISTINCT s FROM Specialist s
        JOIN s.specialities sp
        JOIN com.takecare.backend.specialistschedule.model.SpecialistSchedule sc
          ON sc.specialist.id = s.id
        WHERE LOWER(sp.name) = LOWER(:category)
          AND sc.available = true
          AND sc.dayOfWeek = :dayOfWeek
    """)
    List<Specialist> findBySpecialityNameAndAvailability(
            @Param("category") String category,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );
}