package com.project.gympilot_backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void grantsProtectedEndpointAuthorityOnlyToVerifiedEmailTokens() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var unverified = converter.convert(jwt(false));
        var verified = converter.convert(jwt(true));

        assertThat(unverified.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER")
                .doesNotContain("EMAIL_VERIFIED");
        assertThat(verified.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER", "EMAIL_VERIFIED");
    }

    private Jwt jwt(boolean emailVerified) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("00000000-0000-0000-0000-000000000001")
                .claim("role", "USER")
                .claim("email_verified", emailVerified)
                .build();
    }
}
