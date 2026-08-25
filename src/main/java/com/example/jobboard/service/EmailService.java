package com.example.jobboard.service;

import com.example.jobboard.model.UserSurvey;
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

    public void sendNewUserNotification(String newUserEmail, String displayName) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.info("[NEW USER] name={} email={}", displayName, newUserEmail);
            return;
        }
        String subject = "[PitStop] New user registered: " + newUserEmail;
        String body = "A new user just created an account.\n\nName: " + displayName + "\nEmail: " + newUserEmail;
        try {
            sendViaResend("onboarding@resend.dev", "wensonghu@gmail.com", subject, body);
        } catch (Exception e) {
            logger.error("Failed to send new user notification for {}: {}", newUserEmail, e.getMessage());
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

    public void sendChatStartedNotification(Long userId, Long sessionId, boolean afterHours) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.info("[CHAT STARTED] userId={} sessionId={} afterHours={}", userId, sessionId, afterHours);
            return;
        }
        String prefix  = afterHours ? "⏰ After-hours " : "";
        String subject = "[PitStop] " + prefix + "New live chat from user #" + userId;
        String body = (afterHours
                ? "A user sent a message outside business hours. They have been told to expect a reply within 24 hours.\n\n"
                : "A user has started a live support chat.\n\n")
                + "User ID: " + userId + "\n"
                + "Session ID: " + sessionId + "\n\n"
                + "Open the monitoring panel → Chat tab to respond.\n"
                + baseUrl + "/?admin=1";
        try {
            sendViaResend("onboarding@resend.dev", "wensonghu@gmail.com", subject, body);
        } catch (Exception e) {
            logger.error("Failed to send chat notification: {}", e.getMessage());
        }
    }

    public void sendSurveyNotification(UserSurvey survey) {
        String label = (survey.getEmail() != null && !survey.getEmail().isBlank())
                ? survey.getEmail() : "anonymous";
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.info("[SURVEY] from={}", label);
            return;
        }
        String subject = "📋 New survey response — " + label;
        String body = "New survey response received.\n\n"
                + "Email: "             + label + "\n"
                + "Job search stage: "  + nvl(survey.getJobSearchStage()) + "\n"
                + "Search duration: "   + nvl(survey.getSearchDuration())  + "\n"
                + "Support needed: "    + nvl(survey.getSupportNeed())     + "\n"
                + "Finds it helpful: "  + nvl(survey.getFindHelpful())     + "\n"
                + "Reason: "            + nvl(survey.getHelpfulReason())   + "\n\n"
                + "View all: " + baseUrl + "/?admin=1";
        try {
            sendViaResend("onboarding@resend.dev", "wensonghu@gmail.com", subject, body);
        } catch (Exception e) {
            logger.error("Failed to send survey notification: {}", e.getMessage());
        }
    }

    private String nvl(String s) { return s != null ? s : "—"; }

    public void sendAccountSetupEmail(String toEmail, String name, String setupToken) {
        String setupUrl = baseUrl + "/?setup-token=" + setupToken;
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.info("[ACCOUNT SETUP] email={} url={}", toEmail, setupUrl);
            return;
        }
        String subject = "Your PitStop account is ready";
        String body = "Hi " + name + ",\n\n"
                + "Your PitStop account has been set up.\n\n"
                + "Click here to set your password and sign in:\n\n"
                + setupUrl + "\n\n"
                + "This link expires in 72 hours. Your cards and progress are already saved.\n\n"
                + "— PitStop Support";
        try {
            sendViaResend("onboarding@resend.dev", toEmail, subject, body);
            logger.info("Account setup email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send setup email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send account setup email", e);
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
