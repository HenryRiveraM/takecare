package com.takecare.backend.calification.repository;

import com.takecare.backend.calification.model.Calification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalificationRepository
        extends JpaRepository<Calification, Integer>{

}