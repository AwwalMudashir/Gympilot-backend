package com.project.gympilot_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.auth.dto.AuthResponse;
import com.project.gympilot_backend.auth.dto.RegisterRequest;
import com.project.gympilot_backend.auth.repository.AccountTokenRepository;
import com.project.gympilot_backend.auth.repository.RefreshTokenRepository;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.user.model.User;
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
                mock(LoginAttemptService.class),
                notificationService,
                properties
        );
    }

    @Test
    void registerHashesCredentialsAndCreatesARefreshableSession() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ADA@EXAMPLE.COM", "StrongPass!123", "StrongPass!123");
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            return user;
        });
        when(tokenHashService.generate()).thenReturn("verification-token", "refresh-token");
        when(tokenHashService.hash("verification-token")).thenReturn("a".repeat(64));
        when(tokenHashService.hash("refresh-token")).thenReturn("b".repeat(64));
        when(jwtTokenService.issueAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenService.accessTokenSeconds()).thenReturn(900L);
        when(profileRepository.existsByUserId(any(UUID.class))).thenReturn(false);

        AuthResponse response = authService.register(request, "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("ada@example.com");
        assertThat(response.developmentVerificationToken()).isEqualTo("verification-token");
        verify(accountTokenRepository).save(any());
        verify(refreshTokenRepository).save(any());
        verify(notificationService).sendVerification(any(User.class), org.mockito.ArgumentMatchers.eq("verification-token"));
    }
}
