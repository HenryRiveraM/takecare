package com.takecare.calification.repository;

import com.takecare.calification.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface CalificationRepository
        extends JpaRepository<Report,Integer>{

}