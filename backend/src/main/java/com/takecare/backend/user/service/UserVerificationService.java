package com.takecare.backend.user.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.VerifyUserRequest;
import com.takecare.backend.user.dto.VerifyUserResponse;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@Service
public class UserVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(UserVerificationService.class);

    private final UserRepository userRepository;

    public UserVerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param userId 
     * @param request
     * @return VerifyUserResponse con el estado actualizado
     */
    public VerifyUserResponse verifyUser(Integer userId, VerifyUserRequest request) {
        logger.info("Attempting to verify user with id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found with id: {}", userId);
                    return new RuntimeException("Usuario no encontrado con ID: " + userId);
                });

        if (user.getCiNumber() == null || !user.getCiNumber().trim().equals(request.getCiNumber().trim())) {
            logger.warn("CI mismatch for user id: {}", userId);
            throw new RuntimeException("El número de CI no coincide con los registros del usuario");
        }

        if (request.getCiDocumentImg() != null && !request.getCiDocumentImg().isBlank()) {
            user.setCiDocumentImg(request.getCiDocumentImg());
            logger.debug("CI document image updated for user id: {}", userId);
        }

<<<<<<< Updated upstream
        user.setAccountVerified(1);
=======
        user.setAccountVerified(1); // 1 = verified, 0 = not verified, 2 = pending
>>>>>>> Stashed changes
        user.setLastUpdate(LocalDateTime.now());
        userRepository.save(user);

        logger.info("User with id: {} verified successfully", userId);

        return new VerifyUserResponse(
                user.getId(),
                user.getNames(),
                user.getFirstLastname(),
                user.getEmail(),
<<<<<<< Updated upstream
                user.getAccountVerified() == 1,
=======
                user.getAccountVerified() == 1, // Convert Integer to Boolean: 1 = true (verified)
>>>>>>> Stashed changes
                user.getLastUpdate(),
                "Usuario verificado exitosamente"
        );
    }
}
