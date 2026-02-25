package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.BroadcastMessage;
import com.example.jobboard.repository.AppUserRepository;
import com.example.jobboard.repository.BroadcastMessageRepository;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/broadcast")
public class BroadcastController {

    @Autowired
    private BroadcastMessageRepository broadcastRepo;

    @Autowired
    private AppUserRepository userRepo;

    @Autowired
    private UserService userService;

    // GET /api/broadcast/current — public, returns active message within 48h or 204
    @GetMapping("/current")
    public ResponseEntity<?> getCurrent() {
        return broadcastRepo
                .findTopByActiveTrueAndCreatedAtAfterOrderByCreatedAtDesc(
                        OffsetDateTime.now().minusHours(48))
                .map(m -> ResponseEntity.ok(Map.of(
                        "id", m.getId(),
                        "content", m.getContent(),
                        "createdAt", m.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.noContent().build());
    }

    // POST /api/broadcast — admin only, creates a new broadcast message
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body,
                                    Authentication auth, HttpServletRequest req) {
        AppUser user = resolveUser(auth, req);
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String content = body.get("content");
        if (content == null || content.isBlank() || content.length() > 250) {
            return ResponseEntity.badRequest().build();
        }
        broadcastRepo.deactivateAll();
        BroadcastMessage msg = new BroadcastMessage();
        msg.setContent(content.trim());
        msg.setCreatedBy(user.getId());
        BroadcastMessage saved = broadcastRepo.save(msg);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "content", saved.getContent(),
                "createdAt", saved.getCreatedAt().toString()
        ));
    }

    // DELETE /api/broadcast — admin only, clears active message
    @DeleteMapping
    public ResponseEntity<Void> clear(Authentication auth, HttpServletRequest req) {
        AppUser user = resolveUser(auth, req);
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        broadcastRepo.deactivateAll();
        return ResponseEntity.ok().build();
    }

    // POST /api/broadcast/ack — authenticated, marks current message as seen for this user
    @PostMapping("/ack")
    public ResponseEntity<Void> ack(Authentication auth, HttpServletRequest req) {
        broadcastRepo
                .findTopByActiveTrueAndCreatedAtAfterOrderByCreatedAtDesc(
                        OffsetDateTime.now().minusHours(48))
                .ifPresent(msg -> {
                    AppUser user = resolveUser(auth, req);
                    user.setLastSeenBroadcastId(msg.getId());
                    userRepo.save(user);
                });
        return ResponseEntity.ok().build();
    }

    private AppUser resolveUser(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("appUserId") != null) {
            return userService.findById((Long) session.getAttribute("appUserId"));
        }
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            AppUser user = userService.findByEmail(authentication.getName());
            if (session != null) session.setAttribute("appUserId", user.getId());
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not resolve user");
    }
}
