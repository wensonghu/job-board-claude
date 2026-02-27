package com.example.jobboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/api/auth/verify-email?token=" + token;

        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.warn("Resend API key not configured. Verification link for {}: {}", toEmail, verifyUrl);
            return;
        }

        String subject = "Verify your PitStop account";
        String body = "Welcome to PitStop!\n\n"
                + "Please verify your email address by clicking the link below:\n\n"
                + verifyUrl + "\n\n"
                + "This link expires in 24 hours.\n\n"
                + "If you did not create this account, you can ignore this email.";

        try {
            sendViaResend("onboarding@resend.dev", toEmail, subject, body);
            logger.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendFeedbackEmail(String fromEmail, String category, String message) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.warn("[FEEDBACK] from={} category={} message={}", fromEmail, category, message);
            return;
        }

        String subject = "[PitStop Feedback] " + category + " from " + fromEmail;
        String body = "From: " + fromEmail + "\nCategory: " + category + "\n\n" + message;

        try {
            sendViaResend("onboarding@resend.dev", "wensonghu@gmail.com", subject, body);
            logger.info("Feedback email sent from {}", fromEmail);
        } catch (Exception e) {
            logger.error("Failed to send feedback email from {}: {}", fromEmail, e.getMessage());
            throw new RuntimeException("Failed to send feedback email", e);
        }
    }

    private void sendViaResend(String from, String to, String subject, String text) throws Exception {
        String json = "{"
                + "\"from\":\"PitStop <" + from + ">\","
                + "\"to\":[\"" + to + "\"],"
                + "\"subject\":" + jsonString(subject) + ","
                + "\"text\":" + jsonString(text)
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception("Resend API error " + response.statusCode() + ": " + response.body());
        }
    }

    /** Escapes a string value for embedding in a JSON literal. */
    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                + "\"";
    }
}
