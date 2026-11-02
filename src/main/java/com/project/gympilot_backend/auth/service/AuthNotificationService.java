package com.project.gympilot_backend.auth.service;

import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthNotificationService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppSecurityProperties properties;

    public void sendVerification(User user, String token) {
        send(user.getEmail(), "Verify your GymPilot email",
                "Verify your account: " + properties.getFrontendBaseUrl() + "/verify-email?token=" + token);
    }

    public void sendPasswordReset(User user, String token) {
        send(user.getEmail(), "Reset your GymPilot password",
                "Reset your password: " + properties.getFrontendBaseUrl() + "/reset-password?token=" + token);
    }

    private void send(String recipient, String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Email delivery is not configured; skipped '{}' email to {}", subject, recipient);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (MailException exception) {
            log.warn("Could not send '{}' email to {}", subject, recipient);
        }
    }
}
