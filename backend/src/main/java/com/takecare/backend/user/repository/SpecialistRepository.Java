package com.takecare.user.repository;

import com.takecare.user.model.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialistRepository 
        extends JpaRepository<Specialist, Long> {

}