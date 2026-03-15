package com.takecare.backend.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.takecare.backend.auth.model.PasswordResetToken;
import com.takecare.backend.auth.repository.PasswordResetTokenRepository;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@Service
public class PasswordRecoveryService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    public void generateResetToken(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiration(LocalDateTime.now().plusHours(1));

        tokenRepository.save(resetToken);

        System.out.println("Token generado: " + token);
    }


    public void resetPassword(String token, String newPasswordHash) {

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(newPasswordHash);

        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }
}