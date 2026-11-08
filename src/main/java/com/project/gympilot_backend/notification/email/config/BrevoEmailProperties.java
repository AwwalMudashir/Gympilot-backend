package com.project.gympilot_backend.notification.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.email")
public class BrevoEmailProperties {
    private boolean enabled;
    private final Brevo brevo = new Brevo();

    public boolean isConfigured() {
        return enabled && hasText(brevo.apiKey) && hasText(brevo.senderEmail);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    @Setter
    public static class Brevo {
        private String baseUrl = "https://api.brevo.com/v3";
        private String apiKey = "";
        private String senderEmail = "";
        private String senderName = "GymPilot";
    }
}
