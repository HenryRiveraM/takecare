package com.takecare.backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.auth.DTO.ForgotPasswordRequestDTO;
import com.takecare.backend.auth.DTO.ResetPasswordRequestDTO;
import com.takecare.backend.auth.service.PasswordRecoveryService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(PasswordRecoveryService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO request) {

        passwordRecoveryService.generateResetToken(request.getEmail());
        return ResponseEntity.ok("Se envió el enlace de recuperación");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequestDTO request) {

        passwordRecoveryService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Contraseña actualizada");
    }
}