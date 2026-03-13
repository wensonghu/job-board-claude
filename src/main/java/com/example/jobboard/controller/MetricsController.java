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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Exclude admin + test accounts from all event metrics
        List<Long> excludedIds = jdbc.queryForList(
            "SELECT id FROM app_user WHERE role = 'ADMIN' " +
            "OR email ILIKE '%demotest%' OR email ILIKE '%@pitstop.local'", Long.class);
        String excl = buildExclusionClause(excludedIds);

        Map<String, Object> result = new LinkedHashMap<>();

        // ── Funnel (all-time, excluding admin/test) ───────────────────────────
        result.put("funnel", Map.of(
            "pageViews",        count("page_view",          excl),
            "modalOpens",       count("sign_in_modal_open", excl),
            "registerAttempts", count("register_submit",    excl),
            "registerSuccess",  count("register_success",   excl),
            "signIns",          count("sign_in_success",    excl)
        ));

        // ── Today vs Yesterday (Pacific time) ────────────────────────────────
        result.put("today", Map.of(
            "pageViews",   countToday("page_view",        excl),
            "newAccounts", countToday("register_success", excl),
            "cardsAdded",  countToday("card_add",         excl),
            "signIns",     countToday("sign_in_success",  excl)
        ));
        result.put("yesterday", Map.of(
            "pageViews",   countYesterday("page_view",        excl),
            "newAccounts", countYesterday("register_success", excl),
            "cardsAdded",  countYesterday("card_add",         excl),
            "signIns",     countYesterday("sign_in_success",  excl)
        ));

        // ── Resume review requests ────────────────────────────────────────────
        Long resumeReviewCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action " +
            "WHERE event_type = 'support_request' " +
            "  AND event_data::jsonb->>'category' = 'Resume Review'", Long.class);
        result.put("resumeReviewCount", resumeReviewCount == null ? 0 : resumeReviewCount);

        // ── Active sessions (last 7 days, excluding admin/test) ───────────────
        Long activeSessions = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT session_id) FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '7 days'" + excl, Long.class);
        result.put("activeSessions7d", activeSessions);

        // ── Total users + status breakdown ────────────────────────────────────
        Long totalUsers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app_user", Long.class);
        result.put("totalUsers", totalUsers);

        Long registeredUsers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app_user WHERE status = 'REGISTERED'", Long.class);
        Long pendingUsers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app_user WHERE status = 'PENDING'", Long.class);
        result.put("userBreakdown", Map.of(
            "registered", registeredUsers == null ? 0 : registeredUsers,
            "pending",    pendingUsers    == null ? 0 : pendingUsers
        ));

        // ── Device breakdown (last 30 days, excluding admin/test) ─────────────
        Long mobileCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '30 days' " +
            "AND event_type = 'page_view' " +
            "AND (user_agent ILIKE '%mobile%' OR user_agent ILIKE '%android%' " +
            "     OR user_agent ILIKE '%iphone%' OR user_agent ILIKE '%ipad%')" + excl, Long.class);
        Long desktopCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '30 days' " +
            "AND event_type = 'page_view' " +
            "AND NOT (user_agent ILIKE '%mobile%' OR user_agent ILIKE '%android%' " +
            "         OR user_agent ILIKE '%iphone%' OR user_agent ILIKE '%ipad%')" + excl, Long.class);
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
            "  AND created_at >= NOW() - INTERVAL '14 days'" + excl +
            " GROUP BY day ORDER BY day DESC");
        result.put("dailyViews", daily);

        // ── Top events (last 30 days, excluding admin/test) ───────────────────
        List<Map<String, Object>> topEvents = jdbc.queryForList(
            "SELECT event_type, COUNT(*) AS total, " +
            "       COUNT(DISTINCT session_id) AS unique_sessions " +
            "FROM user_action " +
            "WHERE created_at >= NOW() - INTERVAL '30 days'" + excl +
            " GROUP BY event_type ORDER BY total DESC LIMIT 12");
        result.put("topEvents", topEvents);

        return ResponseEntity.ok(result);
    }

    // ── Generic metric drill-down ─────────────────────────────────────────────

    @GetMapping("/drill-down")
    public ResponseEntity<?> getDrillDown(
            @RequestParam String event,
            @RequestParam(defaultValue = "alltime") String period,
            Authentication auth, HttpServletRequest req) {

        AppUser user = resolveUser(auth, req);
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Long> excludedIds = jdbc.queryForList(
            "SELECT id FROM app_user WHERE role = 'ADMIN' " +
            "OR email ILIKE '%demotest%' OR email ILIKE '%@pitstop.local'", Long.class);
        String excl = buildExclusionClause(excludedIds);

        String periodFilter = switch (period) {
            case "today" ->
                " AND created_at >= date_trunc('day', NOW() AT TIME ZONE 'America/Los_Angeles')" +
                "                   AT TIME ZONE 'America/Los_Angeles'";
            case "yesterday" ->
                " AND created_at >= (date_trunc('day', NOW() AT TIME ZONE 'America/Los_Angeles')" +
                "                    - INTERVAL '1 day') AT TIME ZONE 'America/Los_Angeles'" +
                " AND created_at <   date_trunc('day', NOW() AT TIME ZONE 'America/Los_Angeles')" +
                "                    AT TIME ZONE 'America/Los_Angeles'";
            default -> "";
        };

        List<Map<String, Object>> rows;

        if ("sessions_7d".equals(event)) {
            // Active sessions drill-down: one row per session
            rows = jdbc.queryForList(
                "SELECT session_id, " +
                "       MIN(created_at) AS first_seen, " +
                "       MAX(created_at) AS last_seen, " +
                "       COUNT(*) AS event_count, " +
                "       MAX(user_id_hash) AS user_id_hash " +
                "FROM user_action " +
                "WHERE created_at >= NOW() - INTERVAL '7 days'" + excl +
                " GROUP BY session_id ORDER BY last_seen DESC LIMIT 100");
        } else {
            rows = jdbc.queryForList(
                "SELECT created_at, session_id, user_id_hash, event_data " +
                "FROM user_action WHERE event_type = ?" + excl + periodFilter +
                " ORDER BY created_at DESC LIMIT 100",
                event);
        }

        // Build hash → user lookup from the unique hashes in results
        Set<String> hashes = rows.stream()
            .map(r -> (String) r.get("user_id_hash"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<String, Map<String, Object>> hashToUser = new HashMap<>();
        if (!hashes.isEmpty()) {
            List<Map<String, Object>> allUsers = jdbc.queryForList(
                "SELECT id, email, status FROM app_user");
            for (Map<String, Object> u : allUsers) {
                String h = sha256(u.get("id").toString());
                if (hashes.contains(h)) hashToUser.put(h, u);
            }
        }

        List<Map<String, Object>> enriched = rows.stream().map(row -> {
            Map<String, Object> out = new LinkedHashMap<>(row);
            String hash = (String) row.get("user_id_hash");
            if (hash != null && hashToUser.containsKey(hash)) {
                Map<String, Object> u = hashToUser.get(hash);
                out.put("user_id", u.get("id"));
                out.put("email", u.get("email"));
                out.put("user_status", u.get("status"));
            }
            out.remove("user_id_hash");
            return out;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(enriched);
    }

    // ── Resume review drill-down ──────────────────────────────────────────────

    @GetMapping("/resume-reviews")
    public ResponseEntity<?> getResumeReviews(Authentication auth, HttpServletRequest req) {
        AppUser user = resolveUser(auth, req);
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Map<String, Object>> reviews = jdbc.queryForList(
            "SELECT event_data::jsonb->>'email'   AS email, " +
            "       event_data::jsonb->>'message' AS message, " +
            "       created_at " +
            "FROM user_action " +
            "WHERE event_type = 'support_request' " +
            "  AND event_data::jsonb->>'category' = 'Resume Review' " +
            "ORDER BY created_at DESC");
        return ResponseEntity.ok(reviews);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildExclusionClause(List<Long> ids) {
        if (ids.isEmpty()) return "";
        String hashes = ids.stream()
            .map(id -> "'" + sha256(id.toString()) + "'")
            .collect(Collectors.joining(","));
        return " AND (user_id_hash IS NULL OR user_id_hash NOT IN (" + hashes + "))";
    }

    private long count(String eventType, String excl) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action WHERE event_type = ?" + excl,
            Long.class, eventType);
        return n == null ? 0 : n;
    }

    private long countToday(String eventType, String excl) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action WHERE event_type = ?" + excl +
            " AND created_at >= date_trunc('day', NOW() AT TIME ZONE 'America/Los_Angeles')" +
            "                   AT TIME ZONE 'America/Los_Angeles'",
            Long.class, eventType);
        return n == null ? 0 : n;
    }

    private long countYesterday(String eventType, String excl) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_action WHERE event_type = ?" + excl +
            " AND created_at >= (date_trunc('day', NOW() AT TIME ZONE 'America/Los_Angeles')" +
            "                    - INTERVAL '1 day') AT TIME ZONE 'America/Los_Angeles'" +
            " AND created_at <  date_trunc('day', NOW() AT TIME ZONE 'America/Los_Angeles')" +
            "                   AT TIME ZONE 'America/Los_Angeles'",
            Long.class, eventType);
        return n == null ? 0 : n;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
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
