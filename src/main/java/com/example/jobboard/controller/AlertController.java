package com.example.jobboard.controller;

import com.example.jobboard.dto.AlertDto;
import com.example.jobboard.model.AlertDismissal;
import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.Card;
import com.example.jobboard.repository.AlertDismissalRepository;
import com.example.jobboard.repository.CardRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private AlertDismissalRepository alertDismissalRepository;

    @Autowired
    private UserService userService;

    /**
     * Returns active, non-dismissed interview reminders for the current user.
     * Also prunes stale dismissals so rescheduled interviews re-appear.
     */
    @GetMapping
    public ResponseEntity<List<AlertDto>> getAlerts(Authentication authentication,
                                                    HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);

        List<Card> cards = cardRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(48);

        List<String> activeKeys = new ArrayList<>();
        Map<String, AlertDto> alertMap = new LinkedHashMap<>();

        for (Card card : cards) {
            String interviewDate = card.getInterviewDate();
            if (interviewDate == null || interviewDate.isBlank() || "TBD".equalsIgnoreCase(interviewDate)) continue;
            try {
                String[] parts = interviewDate.split("\\|");
                String datePart = parts[0];
                String timePart = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "23:59";
                LocalDateTime dt = LocalDateTime.parse(datePart + "T" + timePart);
                if (dt.isAfter(now) && dt.isBefore(cutoff)) {
                    String key = "interview-" + card.getId();
                    activeKeys.add(key);
                    String dateDisplay = formatDisplay(datePart, parts.length > 1 ? parts[1] : "");
                    String msg = "Interview scheduled with " + card.getCompany() + " at " + dateDisplay + ". Prepare for interview!";
                    alertMap.put(key, new AlertDto(key, msg));
                }
            } catch (Exception ignored) {}
        }

        // Prune stale dismissals so rescheduled/passed interviews re-appear
        if (activeKeys.isEmpty()) {
            alertDismissalRepository.deleteByUserId(userId);
        } else {
            alertDismissalRepository.deleteByUserIdAndAlertKeyNotIn(userId, activeKeys);
        }

        // Filter out dismissed alerts
        List<AlertDto> result = alertMap.entrySet().stream()
                .filter(e -> !alertDismissalRepository.existsByUserIdAndAlertKey(userId, e.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Persist a dismissal so the alert won't reappear until the interview is rescheduled.
     */
    @PostMapping("/{alertKey}/dismiss")
    public ResponseEntity<Void> dismissAlert(@PathVariable String alertKey,
                                             Authentication authentication,
                                             HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        if (!alertDismissalRepository.existsByUserIdAndAlertKey(userId, alertKey)) {
            AlertDismissal dismissal = new AlertDismissal();
            dismissal.setUserId(userId);
            dismissal.setAlertKey(alertKey);
            alertDismissalRepository.save(dismissal);
        }
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("appUserId") != null) {
            return (Long) session.getAttribute("appUserId");
        }

        String email = authentication.getName();
        if (email != null && !email.isEmpty() && !(authentication instanceof OAuth2AuthenticationToken)) {
            AppUser user = userService.findByEmail(email);
            if (session != null) {
                session.setAttribute("appUserId", user.getId());
            }
            return user.getId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not resolve user");
    }

    private String formatDisplay(String datePart, String timePart) {
        String[] bits = datePart.split("-");
        int year = Integer.parseInt(bits[0]);
        int month = Integer.parseInt(bits[1]);
        int day = Integer.parseInt(bits[2]);
        String date = java.time.LocalDate.of(year, month, day)
                .format(DateTimeFormatter.ofPattern("MMM d"));
        if (timePart != null && !timePart.isBlank()) {
            String[] hm = timePart.split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            return date + String.format(" %d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h >= 12 ? "PM" : "AM");
        }
        return date;
    }
}
