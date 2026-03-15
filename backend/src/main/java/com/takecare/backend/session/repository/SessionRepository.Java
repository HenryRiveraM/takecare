package com.takecare.backend.session.repository;

import com.takecare.backend.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository 
        extends JpaRepository<Session, Integer> {

}