package com.project.gympilot_backend.notification.email.service;

import com.project.gympilot_backend.notification.email.config.BrevoEmailProperties;
import com.project.gympilot_backend.notification.email.model.TransactionalEmail;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BrevoTransactionalEmailService implements TransactionalEmailService {
    private final BrevoEmailProperties properties;
    private final RestClient restClient;

    public BrevoTransactionalEmailService(BrevoEmailProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.getBrevo().getBaseUrl()).build();
    }

    @Override
    public boolean isEnabled() {
        return properties.isConfigured();
    }

    @Override
    public void send(TransactionalEmail email) {
        if (!isEnabled()) {
            throw new IllegalStateException("Brevo email delivery is not fully configured");
        }
        BrevoEmailProperties.Brevo brevo = properties.getBrevo();
        var payload = Map.of(
                "sender", Map.of("name", brevo.getSenderName(), "email", brevo.getSenderEmail()),
                "to", List.of(Map.of("name", email.recipientName(), "email", email.recipientEmail())),
                "subject", email.subject(),
                "textContent", email.textContent(),
                "htmlContent", email.htmlContent()
        );
        restClient.post()
                .uri("/smtp/email")
                .header("api-key", brevo.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
