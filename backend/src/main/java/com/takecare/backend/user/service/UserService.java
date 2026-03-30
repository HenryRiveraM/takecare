package com.takecare.backend.user.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.model.User;

@Service
public class UserService {

    protected static final int ACCOUNT_VERIFIED_REJECTED = 0;
    protected static final int ACCOUNT_VERIFIED_APPROVED = 1;
    protected static final int ACCOUNT_VERIFIED_PENDING = 2;

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
        user.setAccountVerified(ACCOUNT_VERIFIED_PENDING);
        user.setRole(role);

        return user;
    }
}