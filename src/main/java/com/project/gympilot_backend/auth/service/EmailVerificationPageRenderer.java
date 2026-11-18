package com.project.gympilot_backend.auth.service;

import com.project.gympilot_backend.security.config.AppSecurityProperties;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationPageRenderer {
    private final AppSecurityProperties properties;

    public EmailVerificationPageRenderer(AppSecurityProperties properties) {
        this.properties = properties;
    }

    public String success() {
        return page(
                "Email verified",
                "Your email address has been verified successfully.",
                "You can now return to GymBro and sign in.",
                true);
    }

    public String failure() {
        return page(
                "Link unavailable",
                "This verification link is invalid, expired, or has already been used.",
                "Open GymBro and request a new verification email if you still need to verify your account.",
                false);
    }

    private String page(String title, String message, String guidance, boolean successful) {
        String accent = successful ? "#3FA36C" : "#D97706";
        String symbol = successful ? "&#10003;" : "!";
        String appLink = escapeHtml(properties.getFrontendBaseUrl());

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s | GymBro</title>
                  <style>
                    *{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;
                    padding:24px;background:#f3f7f4;color:#172019;font-family:Inter,Arial,sans-serif}
                    main{width:min(100%%,520px);padding:40px 30px;text-align:center;background:#fff;
                    border:1px solid #dfe9e2;border-radius:24px;box-shadow:0 18px 55px rgba(20,49,31,.12)}
                    .brand{display:inline-flex;align-items:center;gap:10px;margin-bottom:26px;font-size:22px;font-weight:800}
                    .mark{display:grid;place-items:center;width:42px;height:42px;border-radius:13px;background:#173e28;color:#fff}
                    .status{display:grid;place-items:center;width:76px;height:76px;margin:0 auto 22px;border-radius:50%%;
                    color:#fff;background:%s;font-size:42px;font-weight:800}h1{margin:0 0 12px;font-size:30px;line-height:1.15}
                    p{margin:0 auto 12px;max-width:420px;color:#536159;line-height:1.65}.button{display:inline-block;
                    margin-top:18px;padding:13px 22px;border-radius:12px;background:#3FA36C;color:#fff;text-decoration:none;font-weight:800}
                    .note{margin-top:22px;font-size:13px;color:#7a857e}
                  </style>
                </head>
                <body>
                  <main>
                    <div class="brand"><span class="mark">GB</span><span>GymBro</span></div>
                    <div class="status">%s</div>
                    <h1>%s</h1>
                    <p>%s</p>
                    <p>%s</p>
                    <a class="button" href="%s">Open GymBro</a>
                    <p class="note">You may safely close this browser tab.</p>
                  </main>
                </body>
                </html>
                """.formatted(title, accent, symbol, title, message, guidance, appLink);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
