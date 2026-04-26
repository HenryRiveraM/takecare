package com.takecare.backend.user.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.model.User;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    protected static final byte ACCOUNT_VERIFIED_REJECTED = 0;
    protected static final byte ACCOUNT_VERIFIED_APPROVED = 1;
    protected static final byte ACCOUNT_VERIFIED_PENDING = 2;

    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    protected <T extends User> T prepareUser(T user, int role) {
        logger.info("Preparing user with email: {} and role: {}", user.getEmail(), role);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        user.setCreatedDate(LocalDateTime.now());
        user.setLastUpdate(LocalDateTime.now());
        user.setStatus((byte) 1);
        user.setStrikes((byte) 0);
        user.setAccountVerified(ACCOUNT_VERIFIED_PENDING);
        user.setRole((byte) role);

        logger.debug("User prepared - email: {}, role: {}, status: active, accountVerified: false", 
                     user.getEmail(), role);
        return user;
    }
}