package com.takecare.user.repository;

import com.takecare.user.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository 
        extends JpaRepository<Patient, Long> {

}