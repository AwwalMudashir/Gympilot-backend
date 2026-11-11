package com.project.gympilot_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.auth.dto.LoginRequest;
import com.project.gympilot_backend.auth.dto.RegisterRequest;
import com.project.gympilot_backend.auth.dto.RegistrationResponse;
import com.project.gympilot_backend.auth.repository.AccountTokenRepository;
import com.project.gympilot_backend.auth.repository.RefreshTokenRepository;
import com.project.gympilot_backend.common.exception.ApiException;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private UserRepository userRepository;
    private UserProfileRepository profileRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AccountTokenRepository accountTokenRepository;
    private PasswordEncoder passwordEncoder;
    private TokenHashService tokenHashService;
    private JwtTokenService jwtTokenService;
    private LoginAttemptService loginAttemptService;
    private AuthNotificationService notificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileRepository = mock(UserProfileRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        accountTokenRepository = mock(AccountTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenHashService = mock(TokenHashService.class);
        jwtTokenService = mock(JwtTokenService.class);
        loginAttemptService = mock(LoginAttemptService.class);
        notificationService = mock(AuthNotificationService.class);
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setExposeDevelopmentTokens(true);

        authService = new AuthService(
                userRepository,
                profileRepository,
                refreshTokenRepository,
                accountTokenRepository,
                passwordEncoder,
                tokenHashService,
                jwtTokenService,
                loginAttemptService,
                notificationService,
                properties
        );
    }

    @Test
    void registerCreatesUnverifiedAccountWithoutCreatingASession() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ADA@EXAMPLE.COM", "StrongPass!123", "StrongPass!123");
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            return user;
        });
        when(tokenHashService.generate()).thenReturn("verification-token");
        when(tokenHashService.hash("verification-token")).thenReturn("a".repeat(64));

        RegistrationResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.developmentVerificationToken()).isEqualTo("verification-token");
        verify(accountTokenRepository).save(any());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtTokenService);
        verify(notificationService).sendVerification(any(User.class), org.mockito.ArgumentMatchers.eq("verification-token"));
    }

    @Test
    void loginRejectsCorrectCredentialsUntilEmailIsVerified() {
        User user = User.builder()
                .email("ada@example.com")
                .passwordHash("bcrypt-hash")
                .firstName("Ada")
                .lastName("Lovelace")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("StrongPass!123", "bcrypt-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ada@example.com", "StrongPass!123"), "127.0.0.1", "JUnit"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode()).isEqualTo("EMAIL_NOT_VERIFIED");
                });

        verify(loginAttemptService).clearFailures(user.getId());
        verifyNoInteractions(jwtTokenService);
        verify(refreshTokenRepository, never()).save(any());
    }
}
