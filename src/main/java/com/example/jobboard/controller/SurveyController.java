package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.UserSurvey;
import com.example.jobboard.repository.UserSurveyRepository;
import com.example.jobboard.service.EmailService;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SurveyController {

    @Autowired private UserSurveyRepository surveyRepo;
    @Autowired private EmailService emailService;
    @Autowired private UserService userService;

    // ── Public ────────────────────────────────────────────────────────────────

    @PostMapping("/api/survey")
    public ResponseEntity<Map<String, Object>> submitSurvey(
            @RequestBody Map<String, String> body) {

        UserSurvey survey = new UserSurvey();
        survey.setJobSearchStage(trimOrNull(body.get("jobSearchStage")));
        survey.setSearchDuration(trimOrNull(body.get("searchDuration")));
        survey.setSupportNeed(trimOrNull(body.get("supportNeed")));
        survey.setFindHelpful(trimOrNull(body.get("findHelpful")));
        survey.setHelpfulReason(trimOrNull(body.get("helpfulReason")));
        survey.setEmail(trimOrNull(body.get("email")));
        surveyRepo.save(survey);

        try { emailService.sendSurveyNotification(survey); }
        catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("saved", true));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/surveys")
    public ResponseEntity<?> listSurveys(Authentication auth, HttpServletRequest req) {
        if (!isAdmin(auth, req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        List<UserSurvey> surveys = surveyRepo.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = surveys.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             s.getId());
            m.put("createdAt",      s.getCreatedAt().toString());
            m.put("email",          s.getEmail());
            m.put("jobSearchStage", s.getJobSearchStage());
            m.put("searchDuration", s.getSearchDuration());
            m.put("supportNeed",    s.getSupportNeed());
            m.put("findHelpful",    s.getFindHelpful());
            m.put("helpfulReason",  s.getHelpfulReason());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of("surveys", result, "total", result.size()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isAdmin(Authentication auth, HttpServletRequest req) {
        try {
            AppUser user = null;
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("appUserId") != null) {
                user = userService.findById((Long) session.getAttribute("appUserId"));
            }
            if (user == null && auth != null) user = userService.findByEmail(auth.getName());
            return user != null && "ADMIN".equals(user.getRole());
        } catch (Exception e) { return false; }
    }
}
