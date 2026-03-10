package com.example.jobboard.controller;

import com.example.jobboard.dto.EventDto;
import com.example.jobboard.model.UserAction;
import com.example.jobboard.repository.UserActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private UserActionRepository userActionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Records a single user action event.
     * Public endpoint — works for anonymous and authenticated users.
     * Failures are swallowed so tracking never breaks the app.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackEvent(@RequestBody EventDto dto, HttpServletRequest request) {
        try {
            UserAction action = new UserAction();
            action.setSessionId(truncate(dto.sessionId(), 64));
            action.setEventType(truncate(dto.eventType(), 100));
            action.setPage(truncate(dto.page(), 500));
            action.setReferrer(truncate(dto.referrer(), 500));
            action.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

            // Resolve authenticated user and hash their ID
            Long userId = resolveUserId(request);
            if (userId != null) {
                action.setUserIdHash(sha256(String.valueOf(userId)));
            }

            // Serialize eventData map to JSON string
            if (dto.eventData() != null && !dto.eventData().isEmpty()) {
                action.setEventData(objectMapper.writeValueAsString(dto.eventData()));
            }

            userActionRepository.save(action);
        } catch (Exception ignored) {
            // Tracking must never throw — silently swallow all errors
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (Long) session.getAttribute("appUserId");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
