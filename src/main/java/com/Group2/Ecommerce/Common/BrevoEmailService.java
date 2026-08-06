package com.Group2.Ecommerce.Common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailService {

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name}")
    private String senderName;

    @Value("${brevo.base-url}")
    private String baseUrl;

    /**
     * Emails the password reset code to the given address.
     *
     * @return true when the email was actually dispatched via Brevo; false
     * when no API key is configured yet (dev mode), in which case the code is
     * only logged so the flow stays testable.
     */
    public boolean sendPasswordResetCode(String toEmail, String code) {
        if (!isConfigured()) {
            log.info("[DEV MODE] Password reset code for {}: {}", toEmail, code);
            return false;
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Your " + senderName + " password reset code",
                "htmlContent", "<p>You requested a password reset on " + senderName + ".</p>"
                        + "<p>Your reset code is:</p>"
                        + "<h2 style=\"letter-spacing: 4px;\">" + code + "</h2>"
                        + "<p>This code expires in 30 minutes. "
                        + "If you didn't request this, you can ignore this email.</p>"
        );

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("api-key", apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            client.post()
                    .uri("/v3/smtp/email")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset email sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"your_brevo_api_key".equals(apiKey);
    }
}
