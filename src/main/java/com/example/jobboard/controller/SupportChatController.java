package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.SupportChatMessage;
import com.example.jobboard.model.SupportChatSession;
import com.example.jobboard.repository.ChatScheduleRepository;
import com.example.jobboard.repository.SupportChatMessageRepository;
import com.example.jobboard.repository.SupportChatSessionRepository;
import com.example.jobboard.service.EmailService;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class SupportChatController {

    @Autowired private SupportChatSessionRepository sessionRepo;
    @Autowired private SupportChatMessageRepository messageRepo;
    @Autowired private UserService userService;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ChatScheduleRepository chatScheduleRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChatAvailabilityController chatAvailability;

    // ─── User endpoints ───────────────────────────────────────────────────────

    /** Start or reopen a chat session. Creates one if none is OPEN. */
    @PostMapping("/api/chat/start")
    public ResponseEntity<Map<String, Object>> startChat(
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        AppUser user = resolveUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Reuse existing OPEN session if one exists
        SupportChatSession session = sessionRepo
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "OPEN")
                .orElseGet(() -> {
                    SupportChatSession s = new SupportChatSession();
                    s.setUserId(user.getId());
                    return sessionRepo.save(s);
                });

        // Post the user's first message if provided
        String firstMessage = body.getOrDefault("message", "").trim();
        if (!firstMessage.isEmpty()) {
            saveMessage(session.getId(), "USER", firstMessage);
        }

        // Check business hours and post after-hours system message if needed
        boolean afterHours = false;
        try {
            String tz = chatAvailability.getTimezone();
            afterHours = !ChatAvailabilityController.isNowAvailable(
                    chatScheduleRepo.findAllByOrderByDayOfWeekAsc(), tz);
            if (afterHours && !firstMessage.isEmpty()) {
                saveMessage(session.getId(), "SYSTEM",
                        "We're currently outside business hours. We'll get back to you within 24 hours.");
            }
        } catch (Exception ignored) {}

        // Notify admin (non-blocking; failure doesn't break the flow)
        final boolean isAfterHours = afterHours;
        try { emailService.sendChatStartedNotification(user.getId(), session.getId(), isAfterHours); }
        catch (Exception ignored) {}

        return ResponseEntity.ok(buildSessionView(session, user));
    }

    /** Poll: returns the user's active session + all messages. */
    @GetMapping("/api/chat/session")
    public ResponseEntity<Map<String, Object>> getSession(HttpServletRequest request) {
        AppUser user = resolveUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return sessionRepo.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "OPEN")
                .map(session -> ResponseEntity.ok(buildSessionView(session, user)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("hasSession", false)));
    }

    /** User sends a message to an open session. */
    @PostMapping("/api/chat/message")
    public ResponseEntity<Void> sendMessage(
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        AppUser user = resolveUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String text = body.getOrDefault("message", "").trim();
        if (text.isEmpty()) return ResponseEntity.badRequest().build();

        return sessionRepo.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "OPEN")
                .map(session -> {
                    saveMessage(session.getId(), "USER", text);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /** List all OPEN chat sessions with latest message preview. */
    @GetMapping("/api/admin/chat/sessions")
    public ResponseEntity<?> getSessions(Authentication auth, HttpServletRequest req) {
        if (!isAdmin(auth, req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        List<SupportChatSession> sessions = sessionRepo.findByStatusOrderByCreatedAtDesc("OPEN");
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId", s.getId());
            m.put("createdAt", s.getCreatedAt().toString());
            // User info
            try {
                AppUser u = userService.findById(s.getUserId());
                m.put("userId", u.getId());
                m.put("userStatus", u.getStatus());
                m.put("userEmail", "PENDING".equals(u.getStatus()) ? null : u.getEmail());
            } catch (Exception ignored) {
                m.put("userId", s.getUserId());
            }
            // Last message preview
            List<SupportChatMessage> msgs = messageRepo.findBySessionIdOrderByCreatedAtAsc(s.getId());
            m.put("messageCount", msgs.size());
            if (!msgs.isEmpty()) {
                SupportChatMessage last = msgs.get(msgs.size() - 1);
                m.put("lastMessage", last.getMessage().length() > 80
                        ? last.getMessage().substring(0, 80) + "…" : last.getMessage());
                m.put("lastSender", last.getSenderType());
                m.put("lastAt", last.getCreatedAt().toString());
            }
            m.put("messages", msgs.stream().map(this::msgMap).toList());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of("sessions", result, "openCount", result.size()));
    }

    /** Admin replies to a session. */
    @PostMapping("/api/admin/chat/session/{id}/reply")
    public ResponseEntity<Void> adminReply(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth, HttpServletRequest req) {
        if (!isAdmin(auth, req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String text = body.getOrDefault("message", "").trim();
        if (text.isEmpty()) return ResponseEntity.badRequest().build();

        return sessionRepo.findById(id).map(session -> {
            saveMessage(session.getId(), "ADMIN", text);
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Admin closes a session. */
    @PostMapping("/api/admin/chat/session/{id}/close")
    public ResponseEntity<Void> closeSession(
            @PathVariable Long id, Authentication auth, HttpServletRequest req) {
        if (!isAdmin(auth, req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return sessionRepo.findById(id).map(session -> {
            session.setStatus("CLOSED");
            session.setClosedAt(LocalDateTime.now());
            sessionRepo.save(session);
            saveMessage(session.getId(), "SYSTEM", "Session closed by support.");
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Admin converts a PENDING user to REGISTERED.
     * Sets name + email, generates a setup link, emails the user.
     */
    @PostMapping("/api/admin/chat/session/{id}/convert")
    public ResponseEntity<?> convertUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth, HttpServletRequest req) {
        if (!isAdmin(auth, req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String name  = body.getOrDefault("name", "").trim();
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (name.isEmpty() || email.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "name and email required"));

        SupportChatSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();

        AppUser user = null;
        try { user = userService.findById(session.getUserId()); } catch (Exception ignored) {}
        if (user == null) return ResponseEntity.notFound().build();

        if (!"PENDING".equals(user.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("error", "User is already registered"));

        // Convert: set real name + email, REGISTERED, generate setup token for password
        String setupToken = UUID.randomUUID().toString();
        user.setDisplayName(name);
        user.setEmail(email);
        user.setUserKey(sha256Hex(email));
        user.setStatus("REGISTERED");
        user.setEmailVerified(true);
        user.setVerificationToken(setupToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(72));
        user.setSessionToken(null);
        user.setSessionTokenExpiresAt(null);
        userService.saveUser(user);

        // Post system message in chat
        saveMessage(session.getId(), "SYSTEM",
                "✓ Account created for " + email + ". A setup link has been sent to their email.");

        // Email the user their setup link
        try { emailService.sendAccountSetupEmail(email, name, setupToken); }
        catch (Exception e) {
            return ResponseEntity.ok(Map.of("converted", true, "emailSent", false));
        }

        return ResponseEntity.ok(Map.of("converted", true, "emailSent", true, "email", email));
    }

    // ─── Complete-setup endpoint (called by user after clicking email link) ───

    @PostMapping("/api/auth/complete-setup")
    public ResponseEntity<?> completeSetup(@RequestBody Map<String, String> body) {
        String token    = body.getOrDefault("token", "").trim();
        String password = body.getOrDefault("password", "").trim();
        if (token.isEmpty() || password.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));

        AppUser user = userService.findBySetupToken(token);
        if (user == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired link"));

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userService.saveUser(user);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildSessionView(SupportChatSession session, AppUser user) {
        List<Map<String, Object>> msgs = messageRepo
                .findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream().map(this::msgMap).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hasSession", true);
        m.put("sessionId", session.getId());
        m.put("status", session.getStatus());
        m.put("messages", msgs);
        return m;
    }

    private Map<String, Object> msgMap(SupportChatMessage msg) {
        return Map.of(
                "id",         msg.getId(),
                "senderType", msg.getSenderType(),
                "message",    msg.getMessage(),
                "createdAt",  msg.getCreatedAt().toString()
        );
    }

    private void saveMessage(Long sessionId, String senderType, String text) {
        SupportChatMessage msg = new SupportChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderType(senderType);
        msg.setMessage(text);
        messageRepo.save(msg);
    }

    private AppUser resolveUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("appUserId");
        if (id == null) return null;
        try { return userService.findById((Long) id); } catch (Exception e) { return null; }
    }

    private boolean isAdmin(Authentication auth, HttpServletRequest req) {
        try { return "ADMIN".equals(resolveUserFromAuth(auth, req).getRole()); }
        catch (Exception e) { return false; }
    }

    private AppUser resolveUserFromAuth(Authentication auth, HttpServletRequest req) {
        AppUser bySession = resolveUser(req);
        if (bySession != null) return bySession;
        if (auth != null) return userService.findByEmail(auth.getName());
        throw new IllegalStateException("Cannot resolve user");
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes());
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
