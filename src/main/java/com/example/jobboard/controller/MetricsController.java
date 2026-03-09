package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserService userService;

    @GetMapping
    public ResponseEntity<?> getMetrics(Authentication auth, HttpServletRequest req) {
        AppUser user = resolveUser(auth, req);
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // ── Funnel (all-time) ─────────────────────────────────────────────────
        result.put("funnel", Map.of(
            "pageViews",        count("page_view"),
            "modalOpens",       count("sign_in_modal_open"),
            "registerAttempts", count("register_submit"),
            "registerSuccess",  count("register_success"),
            "signIns",          count("sign_in_success")
        ));

        // ── This week vs last week ────────────────────────────────────────────
        result.put("thisWeek", Map.of(
            "pageViews",   countThisWeek("page_view"),
            "newAccounts", countThisWeek("register_success"),
            "cardsAdded",  countThisWeek("card_add"),
            "signIns",     countThisWeek("sign_in_success")
        ));
        result.put("lastWeek", Map.of(
            "pageViews",   countLastWeek("page_view"),
            "newAccounts", countLastWeek("register_success"),
            "cardsAdded",  countLastWeek("card_add"),
            "signIns",     countLastWeek("sign_in_success")
        ));

        // ── Active sessions (last 7 days) ─────────────────────────────────────
        Long activeSessions = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT session_id) FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '7 days'", Long.class);
        result.put("activeSessions7d", activeSessions);

        // ── Total registered users ────────────────────────────────────────────
        Long totalUsers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app_user", Long.class);
        result.put("totalUsers", totalUsers);

        // ── Device breakdown (last 30 days) ───────────────────────────────────
        Long mobileCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '30 days' " +
            "AND event_type = 'page_view' " +
            "AND (user_agent ILIKE '%mobile%' OR user_agent ILIKE '%android%' " +
            "     OR user_agent ILIKE '%iphone%' OR user_agent ILIKE '%ipad%')", Long.class);
        Long desktopCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '30 days' " +
            "AND event_type = 'page_view' " +
            "AND NOT (user_agent ILIKE '%mobile%' OR user_agent ILIKE '%android%' " +
            "         OR user_agent ILIKE '%iphone%' OR user_agent ILIKE '%ipad%')", Long.class);
        result.put("devices", Map.of(
            "mobile",  mobileCount  == null ? 0 : mobileCount,
            "desktop", desktopCount == null ? 0 : desktopCount
        ));

        // ── Daily page views (last 14 days) ───────────────────────────────────
        List<Map<String, Object>> daily = jdbc.queryForList(
            "SELECT DATE(created_at AT TIME ZONE 'America/Los_Angeles') AS day, " +
            "       COUNT(*) AS views, " +
            "       COUNT(DISTINCT session_id) AS sessions " +
            "FROM user_action " +
            "WHERE event_type = 'page_view' " +
            "  AND created_at >= NOW() - INTERVAL '14 days' " +
            "GROUP BY day ORDER BY day DESC");
        result.put("dailyViews", daily);

        // ── Top events (last 30 days) ─────────────────────────────────────────
        List<Map<String, Object>> topEvents = jdbc.queryForList(
            "SELECT event_type, COUNT(*) AS total, " +
            "       COUNT(DISTINCT session_id) AS unique_sessions " +
            "FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '30 days' " +
            "GROUP BY event_type ORDER BY total DESC LIMIT 12");
        result.put("topEvents", topEvents);

        return ResponseEntity.ok(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long count(String eventType) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action WHERE event_type = ?", Long.class, eventType);
        return n == null ? 0 : n;
    }

    private long countThisWeek(String eventType) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action WHERE event_type = ? " +
            "AND created_at >= date_trunc('week', NOW())", Long.class, eventType);
        return n == null ? 0 : n;
    }

    private long countLastWeek(String eventType) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action WHERE event_type = ? " +
            "AND created_at >= date_trunc('week', NOW()) - INTERVAL '7 days' " +
            "AND created_at <  date_trunc('week', NOW())", Long.class, eventType);
        return n == null ? 0 : n;
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
            AppUser u = userService.findByEmail(authentication.getName());
            if (session != null) session.setAttribute("appUserId", u.getId());
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not resolve user");
    }
}
