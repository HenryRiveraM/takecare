package com.takecare.backend.user.repository;

import com.takecare.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository 
        extends JpaRepository<User, Integer> {

}