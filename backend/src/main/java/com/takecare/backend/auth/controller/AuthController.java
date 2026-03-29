package com.takecare.backend.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.auth.DTO.ForgotPasswordRequestDTO;
import com.takecare.backend.auth.DTO.ResetPasswordRequestDTO;
import com.takecare.backend.auth.DTO.ApiResponseDTO;
import com.takecare.backend.auth.DTO.LoginRequestDTO;
import com.takecare.backend.auth.DTO.LoginResponseDTO;
import com.takecare.backend.auth.service.AuthService;
import com.takecare.backend.auth.service.PasswordRecoveryService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PasswordRecoveryService passwordRecoveryService;
    private final AuthService authService;

    public AuthController(PasswordRecoveryService passwordRecoveryService, AuthService authService) {
        this.passwordRecoveryService = passwordRecoveryService;
        this.authService = authService;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        
        try {
            LoginResponseDTO response = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new ApiResponseDTO<>(true, response));
        } catch (RuntimeException e) {
            // Credenciales incorrectas
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<LoginResponseDTO>(false, null, e.getMessage()));
        }
    }
}