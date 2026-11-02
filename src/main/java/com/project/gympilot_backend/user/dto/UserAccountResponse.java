package com.project.gympilot_backend.user.dto;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserAccountResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String profilePictureUrl,
        UserRole role,
        UserStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserAccountResponse from(User user) {
        return new UserAccountResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePictureUrl(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
