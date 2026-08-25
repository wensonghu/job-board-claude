package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.GoogleCalendarToken;
import com.example.jobboard.repository.GoogleCalendarTokenRepository;
import com.example.jobboard.service.GoogleOAuthService;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar")
public class GoogleCalendarController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarController.class);
    private static final String STATE_SESSION_KEY = "gcalOauthState";

    @Autowired
    private GoogleCalendarTokenRepository tokenRepository;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private UserService userService;

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

    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        return tokenRepository.findByUserId(userId)
                .map(t -> ResponseEntity.ok(Map.of("connected", true, "googleEmail",
                        t.getGoogleEmail() != null ? t.getGoogleEmail() : "")))
                .orElseGet(() -> ResponseEntity.ok(Map.of("connected", false)));
    }

    @GetMapping("/connect")
    public void connect(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws IOException {
        resolveUserId(authentication, request); // ensures the user is signed in before starting the OAuth round trip
        String state = UUID.randomUUID().toString();
        request.getSession(true).setAttribute(STATE_SESSION_KEY, state);
        response.sendRedirect(googleOAuthService.buildAuthorizationUrl(state));
    }

    @GetMapping("/oauth2/callback")
    public void callback(@RequestParam(required = false) String code,
                          @RequestParam(required = false) String state,
                          @RequestParam(required = false) String error,
                          Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object expectedState = session != null ? session.getAttribute(STATE_SESSION_KEY) : null;

        if (error != null || code == null || expectedState == null || !expectedState.equals(state)) {
            logger.warn("Google Calendar OAuth callback rejected (error={}, stateMatch={})",
                    error, expectedState != null && expectedState.equals(state));
            response.sendRedirect("/?calendar_error=true");
            return;
        }
        session.removeAttribute(STATE_SESSION_KEY);

        Long userId;
        try {
            userId = resolveUserId(authentication, request);
        } catch (ResponseStatusException e) {
            response.sendRedirect("/?calendar_error=true");
            return;
        }

        try {
            GoogleOAuthService.TokenResponse tokenResponse = googleOAuthService.exchangeCode(code);
            String email = googleOAuthService.fetchEmail(tokenResponse.accessToken());

            GoogleCalendarToken token = tokenRepository.findByUserId(userId).orElseGet(GoogleCalendarToken::new);
            token.setUserId(userId);
            token.setGoogleEmail(email);
            token.setAccessToken(tokenResponse.accessToken());
            if (tokenResponse.refreshToken() != null) {
                token.setRefreshToken(tokenResponse.refreshToken());
            }
            token.setTokenExpiry(Instant.now().plusSeconds(tokenResponse.expiresInSeconds()));
            token.setScopeGranted("https://www.googleapis.com/auth/calendar.events");
            tokenRepository.save(token);

            response.sendRedirect("/?calendar_connected=true");
        } catch (Exception e) {
            logger.error("Google Calendar OAuth exchange failed for userId={}: {}", userId, e.getMessage());
            response.sendRedirect("/?calendar_error=true");
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        tokenRepository.findByUserId(userId).ifPresent(token -> {
            googleOAuthService.revoke(token.getAccessToken());
            tokenRepository.deleteByUserId(userId);
        });
        return ResponseEntity.noContent().build();
    }
}
