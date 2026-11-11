package com.project.gympilot_backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserRole;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {
    @Mock private UserRepository userRepository;

    @Test
    void acceptsOnlyExistingActiveAccounts() {
        UUID activeId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        User active = User.builder().role(UserRole.USER).status(UserStatus.ACTIVE).build();
        User inactive = User.builder().role(UserRole.USER).status(UserStatus.INACTIVE).build();
        when(userRepository.findById(activeId)).thenReturn(Optional.of(active));
        when(userRepository.findById(inactiveId)).thenReturn(Optional.of(inactive));

        ActiveAccountJwtValidator validator = new ActiveAccountJwtValidator(userRepository);

        assertThat(validator.validate(jwt(activeId)).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(inactiveId)).hasErrors()).isTrue();
    }

    private Jwt jwt(UUID subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject.toString())
                .claim("role", "USER")
                .build();
    }
}
