package com.project.gympilot_backend.security.config;

import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(
            "invalid_token",
            "The account is inactive or no longer exists",
            null);

    private final UserRepository userRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String tokenRole = jwt.getClaimAsString("role");
            boolean active = userRepository.findById(userId)
                    .map(user -> user.getStatus() == UserStatus.ACTIVE
                            && user.getRole() != null
                            && user.getRole().name().equals(tokenRole))
                    .orElse(false);
            return active
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
        }
    }
}
