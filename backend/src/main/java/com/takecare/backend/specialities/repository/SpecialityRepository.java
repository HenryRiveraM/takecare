package com.takecare.backend.specialities.repository;

import com.takecare.backend.specialities.model.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialityRepository extends JpaRepository<Speciality, Integer> {

    Optional<Speciality> findByName(String name);

    boolean existsByName(String name);

    List<Speciality> findByNameContainingIgnoreCase(String name);
}