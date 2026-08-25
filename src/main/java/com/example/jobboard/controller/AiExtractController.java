package com.example.jobboard.controller;

import com.example.jobboard.dto.ExtractedCardDraft;
import com.example.jobboard.dto.PasteExtractionRequest;
import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.AiCardExtractionService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiExtractController {

    private static final Logger logger = LoggerFactory.getLogger(AiExtractController.class);

    @Autowired
    private AiCardExtractionService aiCardExtractionService;

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

    @PostMapping("/extract-card")
    public ResponseEntity<?> extractCard(@RequestBody PasteExtractionRequest req,
                                          Authentication authentication, HttpServletRequest request) {
        resolveUserId(authentication, request);

        if (req.pastedText() == null || req.pastedText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "empty_text"));
        }

        try {
            ExtractedCardDraft draft = aiCardExtractionService.extract(req.pastedText());
            return ResponseEntity.ok(draft);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "ai_not_configured"));
        } catch (Exception e) {
            logger.error("AI extraction failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "extraction_failed"));
        }
    }
}
