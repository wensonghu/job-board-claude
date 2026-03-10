package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Initialises a guest session on every page load.
 * Creates a PENDING user if none exists for this browser session,
 * and returns trial status so the frontend can gate the sign-in modal.
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

        // If session already has a user (authenticated or previously initialised), return their info
        if (session.getAttribute("appUserId") != null) {
            try {
                AppUser user = userService.findById((Long) session.getAttribute("appUserId"));
                return ResponseEntity.ok(buildTrialInfo(user));
            } catch (Exception e) {
                // User no longer exists — fall through to create a fresh PENDING user
                session.removeAttribute("appUserId");
            }
        }

        String sessionToken = body.getOrDefault("sessionToken", "").trim();
        if (sessionToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

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
