package com.project.gympilot_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.notification.email.model.TransactionalEmail;
import com.project.gympilot_backend.notification.email.service.TransactionalEmailService;
import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.user.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthNotificationServiceTest {

    @Test
    void verificationEmailUsesBrowserSafeApiLink() {
        TransactionalEmailService emailService = mock(TransactionalEmailService.class);
        when(emailService.isEnabled()).thenReturn(true);

        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setPublicApiBaseUrl("https://api.gymbro.example/");
        properties.setFrontendBaseUrl("gymbro://auth");
        AuthNotificationService service = new AuthNotificationService(emailService, properties);

        User user = User.builder()
                .email("ada@example.com")
                .firstName("Ada")
                .lastName("Lovelace")
                .build();

        service.sendVerification(user, "token with+/characters");

        ArgumentCaptor<TransactionalEmail> emailCaptor = ArgumentCaptor.forClass(TransactionalEmail.class);
        verify(emailService).send(emailCaptor.capture());
        TransactionalEmail email = emailCaptor.getValue();

        assertThat(email.textContent()).contains(
                "https://api.gymbro.example/api/auth/verify-email-link?token=token+with%2B%2Fcharacters");
        assertThat(email.htmlContent()).contains("/api/auth/verify-email-link?token=");
        assertThat(email.htmlContent()).doesNotContain("gymbro://auth/verify-email");
    }

    @Test
    void verificationResultPageKeepsAppOpeningOptional() {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setFrontendBaseUrl("gymbro://auth");
        EmailVerificationPageRenderer renderer = new EmailVerificationPageRenderer(properties);

        assertThat(renderer.success())
                .contains("Email verified")
                .contains("href=\"gymbro://auth\"")
                .contains("You may safely close this browser tab");
        assertThat(renderer.failure())
                .contains("invalid, expired, or has already been used")
                .doesNotContain("token=");
    }
}
