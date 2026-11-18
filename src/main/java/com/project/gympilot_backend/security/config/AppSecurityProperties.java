package com.project.gympilot_backend.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    public static final String DEFAULT_DEVELOPMENT_SECRET =
            "OhCpmyaE+035thndxU1icGpdKOA/uGzKJYxIHLp1E7MlwbEx98sS1H1cqBXy0MdEY9l68Uz9GeMb6LQS1diauA==";

    private String jwtSecret = DEFAULT_DEVELOPMENT_SECRET;
    private String jwtIssuer = "gympilot-api";
    private long accessTokenMinutes = 15;
    private long refreshTokenDays = 30;
    private long passwordResetMinutes = 30;
    private long emailVerificationHours = 24;
    private int maxFailedLogins = 5;
    private long lockMinutes = 15;
    private boolean exposeDevelopmentTokens;
    private String frontendBaseUrl = "gymbro://auth";
    private String publicApiBaseUrl = "http://localhost:8080";
}
