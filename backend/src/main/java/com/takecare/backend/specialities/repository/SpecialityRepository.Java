package com.takecare.backend.specialities.repository;

import com.takecare.backend.specialities.model.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialityRepository 
        extends JpaRepository<Speciality, Integer> {

}