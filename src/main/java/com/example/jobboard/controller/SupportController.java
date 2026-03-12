package com.example.jobboard.controller;

import com.example.jobboard.dto.FeedbackRequest;
import com.example.jobboard.model.UserAction;
import com.example.jobboard.repository.UserActionRepository;
import com.example.jobboard.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserActionRepository userActionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody FeedbackRequest req) {
        if (req.getFromEmail() == null || req.getMessage() == null
                || req.getFromEmail().isBlank() || req.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String category = req.getCategory() != null ? req.getCategory() : "General Feedback";

        // Persist server-side record — survives email delivery failures
        try {
            UserAction record = new UserAction();
            record.setEventType("support_request");
            record.setEventData(objectMapper.writeValueAsString(Map.of(
                "email",    req.getFromEmail(),
                "category", category,
                "message",  req.getMessage()
            )));
            userActionRepository.save(record);
        } catch (Exception ignored) {}

        emailService.sendFeedbackEmail(req.getFromEmail(), category, req.getMessage());
        return ResponseEntity.ok().build();
    }
}
