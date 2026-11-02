package com.project.gympilot_backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        AuthUserResponse user,
        String developmentVerificationToken
) {}
