package com.takecare.backend.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takecare.backend.user.model.User;

public interface UserRepository 
        extends JpaRepository<User, Integer> {
                Optional<User> findByEmail(String email);
}