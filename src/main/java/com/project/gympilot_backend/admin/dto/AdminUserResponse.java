package com.project.gympilot_backend.admin.dto;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
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
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePictureUrl(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
