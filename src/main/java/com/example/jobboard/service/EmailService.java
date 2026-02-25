package com.example.jobboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/api/auth/verify-email?token=" + token;

        if (smtpHost == null || smtpHost.isBlank()) {
            // SMTP not configured — log the link so dev can verify manually
            logger.warn("SMTP not configured. Verification link for {}: {}", toEmail, verifyUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your MyJobBoard account");
        message.setText(
                "Welcome to MyJobBoard!\n\n" +
                "Please verify your email address by clicking the link below:\n\n" +
                verifyUrl + "\n\n" +
                "This link expires in 24 hours.\n\n" +
                "If you did not create this account, you can ignore this email."
        );

        try {
            mailSender.send(message);
            logger.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendFeedbackEmail(String fromEmail, String category, String message) {
        if (smtpHost == null || smtpHost.isBlank()) {
            logger.warn("[FEEDBACK] from={} category={} message={}", fromEmail, category, message);
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo("wensonghu@gmail.com");
        msg.setReplyTo(fromEmail);
        msg.setSubject("[PitStop Feedback] " + category + " from " + fromEmail);
        msg.setText("From: " + fromEmail + "\nCategory: " + category + "\n\n" + message);

        try {
            mailSender.send(msg);
            logger.info("Feedback email sent from {}", fromEmail);
        } catch (Exception e) {
            logger.error("Failed to send feedback email from {}: {}", fromEmail, e.getMessage());
            throw new RuntimeException("Failed to send feedback email", e);
        }
    }
}
