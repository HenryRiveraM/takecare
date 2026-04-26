package com.takecare.backend.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.takecare.backend.session.model.Session;

@Repository
public interface SessionRepository 
        extends JpaRepository<Session, Integer> {

}