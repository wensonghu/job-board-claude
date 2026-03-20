package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.ChatSchedule;
import com.example.jobboard.repository.ChatScheduleRepository;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@RestController
public class ChatAvailabilityController {

    @Autowired private ChatScheduleRepository scheduleRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserService userService;

    // ─── Public: current availability + full schedule ────────────────────────

    @GetMapping("/api/chat/availability")
    public ResponseEntity<Map<String, Object>> getAvailability() {
        String timezone = getTimezone();
        List<ChatSchedule> schedule = scheduleRepo.findAllByOrderByDayOfWeekAsc();
        boolean available = isNowAvailable(schedule, timezone);

        List<Map<String, Object>> rows = schedule.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dayOfWeek", s.getDayOfWeek());
            m.put("dayName",   DayOfWeek.of(s.getDayOfWeek()).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            m.put("enabled",   s.getEnabled());
            m.put("openTime",  s.getOpenTime().toString());
            m.put("closeTime", s.getCloseTime().toString());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "available", available,
                "timezone",  timezone,
                "schedule",  rows
        ));
    }

    // ─── Admin: save schedule ─────────────────────────────────────────────────

    @PutMapping("/api/admin/chat/schedule")
    public ResponseEntity<?> saveSchedule(
            @RequestBody Map<String, Object> body,
            Authentication auth, HttpServletRequest req) {

        if (!isAdmin(auth, req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        // Save timezone
        String timezone = (String) body.getOrDefault("timezone", "America/New_York");
        try { ZoneId.of(timezone); } // validate
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid timezone")); }
        jdbc.update("UPDATE app_config SET setting_value = ? WHERE setting_key = 'chat_timezone'", timezone);

        // Save schedule rows
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("schedule");
        if (rows == null || rows.size() != 7)
            return ResponseEntity.badRequest().body(Map.of("error", "Expected 7 schedule rows"));

        for (Map<String, Object> row : rows) {
            int dow       = ((Number) row.get("dayOfWeek")).intValue();
            boolean en    = Boolean.TRUE.equals(row.get("enabled"));
            String open   = (String) row.get("openTime");
            String close  = (String) row.get("closeTime");
            ChatSchedule cs = scheduleRepo.findById((short) dow).orElse(new ChatSchedule());
            cs.setDayOfWeek((short) dow);
            cs.setEnabled(en);
            cs.setOpenTime(LocalTime.parse(open));
            cs.setCloseTime(LocalTime.parse(close));
            scheduleRepo.save(cs);
        }

        return ResponseEntity.ok(Map.of("saved", true));
    }

    // ─── Shared availability check (also used by SupportChatController) ───────

    public static boolean isNowAvailable(List<ChatSchedule> schedule, String timezone) {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            int dow = now.getDayOfWeek().getValue();
            LocalTime t = now.toLocalTime();
            return schedule.stream()
                    .filter(s -> s.getDayOfWeek() == dow)
                    .findFirst()
                    .map(s -> s.getEnabled()
                            && !t.isBefore(s.getOpenTime())
                            && !t.isAfter(s.getCloseTime()))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public String getTimezone() {
        try {
            return jdbc.queryForObject(
                    "SELECT setting_value FROM app_config WHERE setting_key = 'chat_timezone'",
                    String.class);
        } catch (Exception e) {
            return "America/New_York";
        }
    }

    private boolean isAdmin(Authentication auth, HttpServletRequest req) {
        try {
            AppUser user = null;
            // Try session first
            jakarta.servlet.http.HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("appUserId") != null) {
                user = userService.findById((Long) session.getAttribute("appUserId"));
            }
            if (user == null && auth != null) user = userService.findByEmail(auth.getName());
            return user != null && "ADMIN".equals(user.getRole());
        } catch (Exception e) { return false; }
    }
}
