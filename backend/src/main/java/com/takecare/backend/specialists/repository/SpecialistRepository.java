package com.takecare.backend.specialities.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takecare.backend.specialities.model.Specialist;

public interface SpecialistRepository 
        extends JpaRepository<Specialist, Long> {
}