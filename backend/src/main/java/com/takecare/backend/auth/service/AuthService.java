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
     * @param email - email del usuario
     * @param password - contraseña en texto plano
     * @return LoginResponseDTO con datos del usuario si es exitoso
     * @throws RuntimeException si las credenciales son incorrectas o la cuenta no está verificada
     */
    public LoginResponseDTO login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("Cuenta suspendida");
        }

        // Validar estado de verificación de cuenta
        if (user.getAccountVerified() == null) {
            user.setAccountVerified((byte) 2); // Por defecto pendiente si es null
        }

        if (user.getAccountVerified() == 2) {
            throw new RuntimeException("Cuenta pendiente de aprobación");
        }

        if (user.getAccountVerified() == 0) {
            throw new RuntimeException("Cuenta rechazada");
        }

        return new LoginResponseDTO(
                user.getId(),
                user.getNames(),
                user.getEmail(),
                user.getRole(),
                user.getAccountVerified()
        );
    }
}
