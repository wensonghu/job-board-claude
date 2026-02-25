package com.example.jobboard.controller;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.Card;
import com.example.jobboard.service.CardService;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    /**
     * Resolve the AppUser.id from the current request.
     * Works for both OAuth2 (session attribute set by OAuth2LoginSuccessHandler)
     * and form login (session attribute set by formLogin successHandler via email lookup).
     */
    private Long resolveUserId(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        HttpSession session = request.getSession(false);

        // OAuth2 path: appUserId set in session by OAuth2LoginSuccessHandler
        if (session != null && session.getAttribute("appUserId") != null) {
            return (Long) session.getAttribute("appUserId");
        }

        // Form login path: resolve by email, cache in session
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

    @GetMapping
    public List<Card> getAllCards(Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        logger.info("GET /api/cards for userId={}", userId);
        return cardService.getAllCards(userId);
    }

    @PostMapping
    public Card createCard(@RequestBody Card card, Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        return cardService.createCard(card, userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card cardDetails,
                                           Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        Card updated = cardService.updateCard(id, cardDetails, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id,
                                           Authentication authentication, HttpServletRequest request) {
        Long userId = resolveUserId(authentication, request);
        cardService.deleteCard(id, userId);
        return ResponseEntity.noContent().build();
    }
}
