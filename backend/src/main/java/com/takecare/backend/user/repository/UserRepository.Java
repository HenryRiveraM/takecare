package com.takecare.backend.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository 
        extends JpaRepository<User, Integer> {

}