package com.takecare.backend.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.takecare.backend.auth.model.PasswordResetToken;
import com.takecare.backend.auth.repository.PasswordResetTokenRepository;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@Service
@Transactional
public class PasswordRecoveryService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String frontendUrl;

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordResetTokenRepository tokenRepository,
                                   JavaMailSender mailSender,
                                   BCryptPasswordEncoder passwordEncoder,
                                   @Value("${app.frontend.url:http://localhost:4200}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
    }

    public void generateResetToken(String email, String clientUrl){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiration(LocalDateTime.now().plusHours(1));

        tokenRepository.save(resetToken);

        String activeFrontendUrl = (clientUrl != null && !clientUrl.isBlank()) ? clientUrl : this.frontendUrl;

        try {
            sendRecoveryEmail(user.getEmail(), user.getNames(), token, activeFrontendUrl);
            System.out.println("Token generado y enviado por correo: " + token);
        } catch (Exception e) {
            System.err.println("Error crítico al enviar el correo de recuperación: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("El servicio de correo electrónico no se encuentra disponible o las credenciales SMTP son inválidas. Detalle: " + e.getMessage());
        }
    }

    private void sendRecoveryEmail(String to, String name, String token, String targetFrontendUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Recuperación de Contraseña - Take Care");
        
        String resetUrl = targetFrontendUrl + "/reset-password?token=" + token;
        
        String text = "Hola " + name + ",\n\n"
                    + "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en Take Care.\n"
                    + "Para proceder, por favor haz clic en el siguiente enlace:\n\n"
                    + resetUrl + "\n\n"
                    + "Este enlace expirará en 1 hora y solo se puede utilizar una vez.\n\n"
                    + "Si no solicitaste este cambio, puedes ignorar este correo de forma segura.\n\n"
                    + "Atentamente,\n"
                    + "El equipo de Take Care";
                    
        message.setText(text);
        mailSender.send(message);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }
}