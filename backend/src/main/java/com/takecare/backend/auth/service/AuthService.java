package com.takecare.backend.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.auth.DTO.LoginResponseDTO;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Autentica un usuario con email y password
     * @param email - email del usuario
     * @param password - contraseña en texto plano
     * @return LoginResponseDTO con datos del usuario si es exitoso
     * @throws RuntimeException si las credenciales son incorrectas
     */
    public LoginResponseDTO login(String email, String password) {
        // Buscar usuario por email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        // Validar contraseña usando BCrypt
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // Validar que la cuenta esté activa
        if (user.getStatus() == null || !user.getStatus()) {
            throw new RuntimeException("Cuenta inactiva");
        }

        // Retornar datos del usuario sin la contraseña
        return new LoginResponseDTO(
                user.getId(),
                user.getNames(),
                user.getEmail(),
                user.getRole()
        );
    }
}
