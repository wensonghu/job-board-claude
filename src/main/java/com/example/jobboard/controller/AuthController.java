package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication,
                                                              HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            response.put("isSignedIn", false);
            response.put("name", "");
            response.put("email", "");
            response.put("authProvider", "");
            return ResponseEntity.ok(response);
        }

        try {
            AppUser user = resolveUser(authentication, request);
            response.put("isSignedIn", true);
            response.put("name", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
            response.put("email", user.getEmail());
            response.put("authProvider", user.getAuthProvider().name().toLowerCase());
            response.put("role", user.getRole());
            response.put("lastSeenBroadcastId", user.getLastSeenBroadcastId());
            response.put("userType", user.getUserType());
            logger.info("Auth status: signed in as {} ({})", user.getEmail(), user.getAuthProvider());
        } catch (Exception e) {
            logger.warn("Auth status: authenticated but could not resolve user — {}", e.getMessage());
            response.put("isSignedIn", false);
            response.put("name", "");
            response.put("email", "");
            response.put("authProvider", "");
        }

        return ResponseEntity.ok(response);
    }

    private AppUser resolveUser(Authentication authentication, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("appUserId") != null) {
            return userService.findById((Long) session.getAttribute("appUserId"));
        }

        // Form login: email is the principal name
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            AppUser user = userService.findByEmail(authentication.getName());
            if (session != null) session.setAttribute("appUserId", user.getId());
            return user;
        }

        throw new IllegalStateException("Cannot resolve user from authentication");
    }
}
