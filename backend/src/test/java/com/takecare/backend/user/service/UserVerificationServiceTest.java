package com.takecare.backend.user.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takecare.backend.user.dto.VerifyUserRequest;
import com.takecare.backend.user.dto.VerifyUserResponse;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserVerificationService userVerificationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setNames("Juan");
        mockUser.setFirstLastname("Perez");
        mockUser.setEmail("juan@example.com");
        mockUser.setCiNumber("12345678");
        mockUser.setBirthDate(LocalDate.of(1990, 5, 20));
<<<<<<< Updated upstream
        mockUser.setAccountVerified(0);
=======
        mockUser.setAccountVerified(0); // 0 = not verified
>>>>>>> Stashed changes
        mockUser.setRole(1);
    }

    @Test
    @DisplayName("HU06 – Verificación exitosa con CI correcta")
    void verifyUser_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyUserRequest request = new VerifyUserRequest();
        request.setCiNumber("12345678");

        VerifyUserResponse response = userVerificationService.verifyUser(1, request);

        assertThat(response.getAccountVerified()).isTrue();
        assertThat(response.getUserId()).isEqualTo(1);
        assertThat(response.getMessage()).contains("exitosamente");
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("HU06 – Verificación exitosa actualiza ciDocumentImg si se envía")
    void verifyUser_updatesCiDocumentImg() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyUserRequest request = new VerifyUserRequest();
        request.setCiNumber("12345678");
        request.setCiDocumentImg("base64encodedimage==");

        userVerificationService.verifyUser(1, request);

        assertThat(mockUser.getCiDocumentImg()).isEqualTo("base64encodedimage==");
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("HU06 – Usuario no encontrado lanza RuntimeException")
    void verifyUser_userNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        VerifyUserRequest request = new VerifyUserRequest();
        request.setCiNumber("12345678");

        assertThatThrownBy(() -> userVerificationService.verifyUser(99, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU06 – CI incorrecta lanza RuntimeException")
    void verifyUser_ciMismatch() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        VerifyUserRequest request = new VerifyUserRequest();
        request.setCiNumber("99999999"); // CI incorrecta

        assertThatThrownBy(() -> userVerificationService.verifyUser(1, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CI");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU06 – accountVerified permanece false si falla la validación")
    void verifyUser_accountRemainsUnverifiedOnFailure() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        VerifyUserRequest request = new VerifyUserRequest();
        request.setCiNumber("00000000");

        assertThatThrownBy(() -> userVerificationService.verifyUser(1, request))
                .isInstanceOf(RuntimeException.class);

        assertThat(mockUser.getAccountVerified()).isEqualTo(0);
    }
}
