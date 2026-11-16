package com.project.gympilot_backend.auth.service;

import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.notification.email.model.TransactionalEmail;
import com.project.gympilot_backend.notification.email.service.TransactionalEmailService;
import com.project.gympilot_backend.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthNotificationService {
    private final TransactionalEmailService emailService;
    private final AppSecurityProperties properties;

    public AuthNotificationService(TransactionalEmailService emailService, AppSecurityProperties properties) {
        this.emailService = emailService;
        this.properties = properties;
    }

    public void sendVerification(User user, String token) {
        String link = properties.getFrontendBaseUrl() + "/verify-email?token=" + token;
        send(user, "Verify your GymBro email", link,
                "Verify your email to finish setting up your GymBro account.", "Verify email");
    }

    public void sendPasswordReset(User user, String token) {
        String link = properties.getFrontendBaseUrl() + "/reset-password?token=" + token;
        send(user, "Reset your GymBro password", link,
                "Use this secure link to choose a new GymBro password.", "Reset password");
    }

    private void send(User user, String subject, String link, String message, String actionLabel) {
        if (!emailService.isEnabled()) {
            log.info("Brevo email delivery is disabled; skipped '{}' email to {}", subject, user.getEmail());
            return;
        }
        String recipientName = user.getFirstName() + " " + user.getLastName();
        String text = message + System.lineSeparator() + link;
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;color:#121814\">"
                + "<h1 style=\"color:#3FA36C\">GymBro</h1>"
                + "<p>Hi " + escapeHtml(user.getFirstName()) + ",</p>"
                + "<p>" + escapeHtml(message) + "</p>"
                + "<p><a href=\"" + escapeHtml(link) + "\" style=\"display:inline-block;background:#3FA36C;"
                + "color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none;font-weight:700\">"
                + escapeHtml(actionLabel) + "</a></p>"
                + "<p style=\"font-size:12px;color:#66706A\">If the button does not open, copy this link:<br>"
                + escapeHtml(link) + "</p></div>";
        try {
            emailService.send(new TransactionalEmail(
                    user.getEmail(), recipientName, subject, text, html));
        } catch (RuntimeException exception) {
            log.warn("Could not send '{}' email to {} through Brevo", subject, user.getEmail());
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
