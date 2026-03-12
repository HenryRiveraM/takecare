package com.takecare.specialities.repository;

import com.takecare.specialities.model.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialityRepository 
        extends JpaRepository<Speciality, Integer> {

}