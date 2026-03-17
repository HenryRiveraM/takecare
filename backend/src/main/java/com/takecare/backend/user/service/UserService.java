package com.takecare.backend.user.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.model.User;

@Service
public class UserService {

    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    protected <T extends User> T prepareUser(T user, int role) {
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        user.setCreatedDate(LocalDateTime.now());
        user.setLastUpdate(LocalDateTime.now());
        user.setStatus(true);
        user.setStrikes(0);
        user.setAccountVerified(false);
        user.setRole(role);

        return user;
    }
}