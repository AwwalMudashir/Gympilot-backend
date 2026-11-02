package com.project.gympilot_backend.security.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator implements ApplicationRunner {

    private final AppSecurityProperties properties;

    public ProductionSecurityValidator(AppSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (AppSecurityProperties.DEFAULT_DEVELOPMENT_SECRET.equals(properties.getJwtSecret())
                || properties.getJwtSecret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be a unique value of at least 32 characters in production");
        }
        if (properties.isExposeDevelopmentTokens()) {
            throw new IllegalStateException("Development account tokens cannot be exposed in production");
        }
    }
}
