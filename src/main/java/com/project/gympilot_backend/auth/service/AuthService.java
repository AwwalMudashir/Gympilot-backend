package com.project.gympilot_backend.auth.service;

import com.project.gympilot_backend.auth.dto.AuthResponse;
import com.project.gympilot_backend.auth.dto.AuthUserResponse;
import com.project.gympilot_backend.auth.dto.ChangePasswordRequest;
import com.project.gympilot_backend.auth.dto.LoginRequest;
import com.project.gympilot_backend.auth.dto.RegisterRequest;
import com.project.gympilot_backend.auth.dto.ResetPasswordRequest;
import com.project.gympilot_backend.auth.dto.TokenDeliveryResponse;
import com.project.gympilot_backend.auth.model.AccountToken;
import com.project.gympilot_backend.auth.model.AccountTokenPurpose;
import com.project.gympilot_backend.auth.model.RefreshToken;
import com.project.gympilot_backend.auth.repository.AccountTokenRepository;
import com.project.gympilot_backend.auth.repository.RefreshTokenRepository;
import com.project.gympilot_backend.common.exception.BadRequestException;
import com.project.gympilot_backend.common.exception.ConflictException;
import com.project.gympilot_backend.common.exception.UnauthorizedException;
import com.project.gympilot_backend.profile.repository.UserProfileRepository;
import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashService tokenHashService;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptService loginAttemptService;
    private final AuthNotificationService notificationService;
    private final AppSecurityProperties properties;

    @Transactional
    public AuthResponse register(RegisterRequest request, String ip, String userAgent) {
        ensurePasswordsMatch(request.password(), request.confirmPassword());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "An account already exists for this email");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();
        user = userRepository.save(user);
        String verificationToken = createAccountToken(user, AccountTokenPurpose.EMAIL_VERIFICATION,
                properties.getEmailVerificationHours(), ChronoUnit.HOURS);
        notificationService.sendVerification(user, verificationToken);
        return issueSession(user, ip, userAgent,
                properties.isExposeDevelopmentTokens() ? verificationToken : null);
    }

    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);
        unlockIfExpired(user);
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new com.project.gympilot_backend.common.exception.ApiException(
                    HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is temporarily locked; try again later");
        }
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user.getStatus() == UserStatus.ACTIVE) {
                loginAttemptService.recordFailure(user.getId());
            }
            throw invalidCredentials();
        }
        User authenticated = loginAttemptService.recordSuccess(user.getId());
        return issueSession(authenticated, ip, userAgent, null);
    }

    @Transactional
    public AuthResponse refresh(String rawToken, String ip, String userAgent) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawToken))
                .orElseThrow(this::invalidRefreshToken);
        if (!stored.isActive(now) || stored.getUser().getStatus() != UserStatus.ACTIVE) {
            throw invalidRefreshToken();
        }
        stored.setRevokedAt(now);
        refreshTokenRepository.save(stored);
        return issueSession(stored.getUser(), ip, userAgent, null);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public TokenDeliveryResponse forgotPassword(String email) {
        final String[] exposed = {null};
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() != UserStatus.INACTIVE)
                .ifPresent(user -> {
                    String token = createAccountToken(user, AccountTokenPurpose.PASSWORD_RESET,
                            properties.getPasswordResetMinutes(), ChronoUnit.MINUTES);
                    notificationService.sendPasswordReset(user, token);
                    if (properties.isExposeDevelopmentTokens()) exposed[0] = token;
                });
        return new TokenDeliveryResponse(true, exposed[0]);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        ensurePasswordsMatch(request.newPassword(), request.confirmPassword());
        AccountToken token = requireUsableAccountToken(request.token(), AccountTokenPurpose.PASSWORD_RESET);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setStatus(UserStatus.ACTIVE);
        token.setConsumedAt(Instant.now());
        userRepository.save(user);
        accountTokenRepository.save(token);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    @Transactional
    public TokenDeliveryResponse resendVerification(String email) {
        final String[] exposed = {null};
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() != UserStatus.INACTIVE && !user.isEmailVerified())
                .ifPresent(user -> {
                    String token = createAccountToken(user, AccountTokenPurpose.EMAIL_VERIFICATION,
                            properties.getEmailVerificationHours(), ChronoUnit.HOURS);
                    notificationService.sendVerification(user, token);
                    if (properties.isExposeDevelopmentTokens()) exposed[0] = token;
                });
        return new TokenDeliveryResponse(true, exposed[0]);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountToken token = requireUsableAccountToken(rawToken, AccountTokenPurpose.EMAIL_VERIFICATION);
        token.getUser().setEmailVerified(true);
        token.setConsumedAt(Instant.now());
        userRepository.save(token.getUser());
        accountTokenRepository.save(token);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        ensurePasswordsMatch(request.newPassword(), request.confirmPassword());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("CURRENT_PASSWORD_INVALID", "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("PASSWORD_REUSED", "New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    private AuthResponse issueSession(User user, String ip, String userAgent, String verificationToken) {
        String rawRefreshToken = tokenHashService.generate();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS))
                .createdByIp(truncate(ip, 64))
                .userAgent(truncate(userAgent, 512))
                .build();
        refreshTokenRepository.save(refreshToken);
        boolean profileExists = profileRepository.existsByUserId(user.getId());
        return new AuthResponse(jwtTokenService.issueAccessToken(user), rawRefreshToken, "Bearer",
                jwtTokenService.accessTokenSeconds(), AuthUserResponse.from(user, profileExists), verificationToken);
    }

    private String createAccountToken(User user, AccountTokenPurpose purpose, long amount, ChronoUnit unit) {
        String raw = tokenHashService.generate();
        accountTokenRepository.save(AccountToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(raw))
                .purpose(purpose)
                .expiresAt(Instant.now().plus(amount, unit))
                .build());
        return raw;
    }

    private AccountToken requireUsableAccountToken(String raw, AccountTokenPurpose purpose) {
        AccountToken token = accountTokenRepository.findByTokenHashAndPurpose(tokenHashService.hash(raw), purpose)
                .orElseThrow(() -> new BadRequestException("TOKEN_INVALID", "Token is invalid or expired"));
        if (!token.isUsable(Instant.now())) {
            throw new BadRequestException("TOKEN_INVALID", "Token is invalid or expired");
        }
        return token;
    }

    private void unlockIfExpired(User user) {
        if (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null
                && !user.getLockedUntil().isAfter(Instant.now())) {
            loginAttemptService.recordSuccess(user.getId());
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
    }

    private void ensurePasswordsMatch(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new BadRequestException("PASSWORDS_DO_NOT_MATCH", "Password confirmation does not match");
        }
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Email or password is incorrect");
    }

    private UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("REFRESH_TOKEN_INVALID", "Refresh token is invalid or expired");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
