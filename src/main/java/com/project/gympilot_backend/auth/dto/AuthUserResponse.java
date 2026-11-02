package com.project.gympilot_backend.auth.dto;

import com.project.gympilot_backend.user.model.User;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String profilePictureUrl,
        boolean emailVerified,
        boolean onboardingComplete
) {
    public static AuthUserResponse from(User user, boolean onboardingComplete) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getProfilePictureUrl(), user.isEmailVerified(), onboardingComplete);
    }
}
