package com.takecare.session.repository;

import com.takecare.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository 
        extends JpaRepository<Session, Integer> {

}