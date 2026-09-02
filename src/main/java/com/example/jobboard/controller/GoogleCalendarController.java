package com.example.jobboard.controller;

import com.example.jobboard.dto.ImportCalendarEventRequest;
import com.example.jobboard.dto.UnmatchedCalendarEvent;
import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.Card;
import com.example.jobboard.model.GoogleCalendarToken;
import com.example.jobboard.repository.CardRepository;
import com.example.jobboard.repository.GoogleCalendarTokenRepository;
import com.example.jobboard.service.CardService;
import com.example.jobboard.service.GoogleCalendarClient;
import com.example.jobboard.service.GoogleOAuthService;
import com.example.jobboard.service.UserService;
import com.example.jobboard.util.InterviewDateMapper;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private GoogleCalendarClient calendarClient;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardService cardService;

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

    /** Confirmed Calendar events (last 14 days -> next 60 days) not yet linked to any PitStop card. */
    @GetMapping("/unmatched-events")
    public ResponseEntity<?> unmatchedEvents(Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        GoogleCalendarToken token = tokenRepository.findByUserId(userId).orElse(null);
        if (token == null) {
            return ResponseEntity.ok(List.of());
        }

        try {
            String accessToken = googleOAuthService.getValidAccessToken(token);
            List<GoogleCalendarClient.EventDetail> events = fetchRecentAndUpcoming(accessToken, token);

            Set<String> alreadyLinked = cardRepository.findByUserIdAndGoogleEventIdIsNotNull(userId)
                    .stream().map(Card::getGoogleEventId).collect(Collectors.toSet());

            List<UnmatchedCalendarEvent> result = events.stream()
                    .filter(e -> e.id() != null && !alreadyLinked.contains(e.id()))
                    .map(e -> new UnmatchedCalendarEvent(
                            e.id(), e.summary(), e.description(), e.organizerEmail(),
                            guessCompanyFromEmail(e.organizerEmail()),
                            InterviewDateMapper.fromCalendarEvent(
                                    new GoogleCalendarClient.RawEvent(e.id(), "confirmed", e.startDate(), e.startDateTime(), e.startTimeZone()))
                    ))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to list unmatched Calendar events for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "calendar_list_failed"));
        }
    }

    // Common non-company senders — don't guess "Greenhouse" or "Gmail" as the hiring company.
    private static final Set<String> NON_COMPANY_EMAIL_DOMAINS = Set.of(
            "gmail.com", "googlemail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com",
            "calendar.google.com", "resource.calendar.google.com",
            "calendly.com", "greenhouse.io", "lever.co", "myworkday.com", "icims.com",
            "smartrecruiters.com", "ashbyhq.com", "zoom.us"
    );

    /** Best-effort company guess from the invite organizer's email domain — the user can always correct it before saving. */
    private String guessCompanyFromEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (NON_COMPANY_EMAIL_DOMAINS.contains(domain)) return null;
        String name = domain.split("\\.")[0];
        if (name.isBlank()) return null;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private List<GoogleCalendarClient.EventDetail> fetchRecentAndUpcoming(String accessToken, GoogleCalendarToken token)
            throws Exception {
        try {
            return calendarClient.listRecentAndUpcoming(accessToken);
        } catch (GoogleCalendarClient.GoogleAuthException e) {
            String refreshed = googleOAuthService.forceRefreshAccessToken(token);
            return calendarClient.listRecentAndUpcoming(refreshed);
        }
    }

    /** Creates a card from a user-reviewed Calendar event and links it — the card's own future edits then sync back normally. */
    @PostMapping("/import-event")
    public ResponseEntity<?> importEvent(@RequestBody ImportCalendarEventRequest req,
                                          Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);

        boolean alreadyImported = cardRepository.findByUserIdAndGoogleEventIdIsNotNull(userId).stream()
                .anyMatch(c -> req.googleEventId().equals(c.getGoogleEventId()));
        if (alreadyImported) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "already_imported"));
        }

        Card card = new Card();
        // Set BEFORE createCard() so the push-sync hook updates the existing event instead of creating a duplicate.
        card.setGoogleEventId(req.googleEventId());
        card.setCompany(req.company());
        card.setPosition(req.position());
        card.setStage(req.stage());
        card.setStatus(req.status());
        card.setInterviewDate(req.interviewDate());
        card.setReferredBy(req.referredBy());
        card.setDetails(req.details());
        if (req.appliedDate() != null && !req.appliedDate().isBlank()) {
            card.setAppliedDate(LocalDate.parse(req.appliedDate()));
        }

        Card saved = cardService.createCard(card, userId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        try {
            tokenRepository.findByUserId(userId).ifPresent(token -> {
                googleOAuthService.revoke(token.getAccessToken());
                tokenRepository.delete(token);
            });
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Calendar disconnect failed for userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "disconnect_failed", "detail", String.valueOf(e.getMessage())));
        }
    }
}
