package com.project.gympilot_backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.auth.service.AuthService;
import com.project.gympilot_backend.auth.service.EmailVerificationPageRenderer;
import com.project.gympilot_backend.security.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerTest {
    private AuthService authService;
    private EmailVerificationPageRenderer pageRenderer;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        pageRenderer = mock(EmailVerificationPageRenderer.class);
        controller = new AuthController(authService, mock(CurrentUserService.class), pageRenderer);
    }

    @Test
    void browserVerificationReturnsSuccessPageWithoutRedirectingToTheApp() {
        when(pageRenderer.success()).thenReturn("<html>Email verified</html>");

        ResponseEntity<String> response = controller.verifyEmailLink("valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Email verified");
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getHeaders().getFirst("Content-Security-Policy")).contains("default-src 'none'");
        verify(authService).verifyEmail("valid-token");
    }

    @Test
    void missingTokenStillReturnsAReadableBrowserPage() {
        when(pageRenderer.failure()).thenReturn("<html>Link unavailable</html>");

        ResponseEntity<String> response = controller.verifyEmailLink("  ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Link unavailable");
        verifyNoInteractions(authService);
    }
}
