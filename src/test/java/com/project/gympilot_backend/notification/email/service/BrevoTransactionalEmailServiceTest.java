package com.project.gympilot_backend.notification.email.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.project.gympilot_backend.notification.email.config.BrevoEmailProperties;
import com.project.gympilot_backend.notification.email.model.TransactionalEmail;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoTransactionalEmailServiceTest {

    @Test
    void sendsBrevoTransactionalEmailWithServerSideApiKey() {
        BrevoEmailProperties properties = new BrevoEmailProperties();
        properties.setEnabled(true);
        properties.getBrevo().setBaseUrl("https://api.brevo.com/v3");
        properties.getBrevo().setApiKey("test-api-key");
        properties.getBrevo().setSenderEmail("hello@gympilot.example");
        properties.getBrevo().setSenderName("GymPilot");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoTransactionalEmailService service = new BrevoTransactionalEmailService(properties, builder);

        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(content().json("""
                        {
                          "sender":{"name":"GymPilot","email":"hello@gympilot.example"},
                          "to":[{"name":"Gym Member","email":"member@example.com"}],
                          "subject":"Verify your GymPilot email",
                          "textContent":"Open the link",
                          "htmlContent":"<p>Open the link</p>"
                        }
                        """))
                .andRespond(withSuccess("{\"messageId\":\"message-1\"}", MediaType.APPLICATION_JSON));

        service.send(new TransactionalEmail(
                "member@example.com",
                "Gym Member",
                "Verify your GymPilot email",
                "Open the link",
                "<p>Open the link</p>"));

        server.verify();
    }
}
