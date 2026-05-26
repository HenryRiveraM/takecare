package com.takecare.backend.auth.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.takecare.backend.auth.DTO.LoginResponseDTO;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);

        user = new User();
        user.setId(4);
        user.setNames("Paciente");
        user.setEmail("paciente@example.com");
        user.setPasswordHash("hash");
        user.setRole((byte) 1);
        user.setAccountVerified((byte) 1);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
    }

    @Test
    void suspendedStatusDeniesLoginRegardlessOfStrikes() {
        user.setStatus((byte) 0);
        user.setStrikes((byte) 1);

        assertThatThrownBy(() -> authService.login(user.getEmail(), "password123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cuenta suspendida");
    }

    @Test
    void activeStatusAllowsLoginWithAccumulatedStrikes() {
        user.setStatus((byte) 1);
        user.setStrikes((byte) 4);

        LoginResponseDTO response = authService.login(user.getEmail(), "password123");

        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }
}
