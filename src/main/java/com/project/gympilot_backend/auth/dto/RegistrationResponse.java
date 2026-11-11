package com.project.gympilot_backend.auth.dto;

import java.util.UUID;

public record RegistrationResponse(
        UUID userId,
        String email,
        boolean verificationRequired,
        String developmentVerificationToken
) {
}
