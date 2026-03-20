package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Initialises a session on every page load using the browser's persistent ps_sid token.
 *
 * For REGISTERED users with a valid 90-day token: restores the session so the user
 * is auto-authenticated without needing Spring Security cookies (which are unreliable
 * behind Railway's reverse proxy).
 *
 * For PENDING (guest trial) users: creates or reconnects their trial account.
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Autowired
    private UserService userService;

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> init(@RequestBody Map<String, String> body,
                                                     HttpServletRequest request) {
        HttpSession session = request.getSession(true);

        String sessionToken = body.getOrDefault("sessionToken", "").trim();

        // Always store the client's token in the server session so login handlers can retrieve it
        if (!sessionToken.isEmpty()) {
            session.setAttribute("guestSessionToken", sessionToken);
        }

        // If session already has a user (e.g. active JSESSIONID), return their info
        if (session.getAttribute("appUserId") != null) {
            try {
                AppUser user = userService.findById((Long) session.getAttribute("appUserId"));
                return ResponseEntity.ok(buildTrialInfo(user));
            } catch (Exception e) {
                // User no longer exists — fall through to re-initialise
                session.removeAttribute("appUserId");
            }
        }

        if (sessionToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Look up user by the persistent browser token
        AppUser byToken = userService.findBySessionToken(sessionToken).orElse(null);
        if (byToken != null) {
            if ("REGISTERED".equals(byToken.getStatus())) {
                if (byToken.getSessionTokenExpiresAt() != null
                        && byToken.getSessionTokenExpiresAt().isAfter(LocalDateTime.now())) {
                    // Valid 90-day token — restore session, no password prompt needed
                    session.setAttribute("appUserId", byToken.getId());
                    return ResponseEntity.ok(buildTrialInfo(byToken));
                } else {
                    // Expired token — clear it and ask user to sign in again
                    userService.clearSessionToken(byToken);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("isPending", false);
                    m.put("trialDaysLeft", 0);
                    m.put("trialExpired", false);
                    return ResponseEntity.ok(m);
                }
            }
            // PENDING user — reconnect their trial session
            session.setAttribute("appUserId", byToken.getId());
            return ResponseEntity.ok(buildTrialInfo(byToken));
        }

        // No user found — create a new PENDING guest account
        AppUser guest = userService.initGuestSession(sessionToken);
        session.setAttribute("appUserId", guest.getId());
        return ResponseEntity.ok(buildTrialInfo(guest));
    }

    private Map<String, Object> buildTrialInfo(AppUser user) {
        boolean isPending = "PENDING".equals(user.getStatus());
        long daysUsed = isPending
                ? ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now())
                : 0;
        long trialDaysLeft = Math.max(0, 7 - daysUsed);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("isPending", isPending);
        m.put("trialDaysLeft", trialDaysLeft);
        m.put("trialExpired", isPending && trialDaysLeft <= 0);
        return m;
    }
}
