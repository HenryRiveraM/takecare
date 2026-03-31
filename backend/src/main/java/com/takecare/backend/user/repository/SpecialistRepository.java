package com.takecare.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.takecare.backend.user.model.Specialist;
@Repository
public interface SpecialistRepository 
        extends JpaRepository<Specialist, Integer> {

}