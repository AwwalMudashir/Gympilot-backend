package com.project.gympilot_backend.notification.email.model;

public record TransactionalEmail(
        String recipientEmail,
        String recipientName,
        String subject,
        String textContent,
        String htmlContent
) {
}
