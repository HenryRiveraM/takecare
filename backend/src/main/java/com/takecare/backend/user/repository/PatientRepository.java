package com.takecare.backend.user.repository;

import com.takecare.backend.user.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
}