package com.project.gympilot_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cloudinary")
public class CloudinaryProperties {
    private String cloudName = "";
    private String apiKey = "";
    private String apiSecret = "";
    private String folder = "gympilot";
    private long maxImageBytes = 5 * 1024 * 1024;

    public boolean isConfigured() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
