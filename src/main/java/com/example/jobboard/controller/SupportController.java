package com.example.jobboard.controller;

import com.example.jobboard.dto.FeedbackRequest;
import com.example.jobboard.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody FeedbackRequest req) {
        if (req.getFromEmail() == null || req.getMessage() == null
                || req.getFromEmail().isBlank() || req.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        emailService.sendFeedbackEmail(
                req.getFromEmail(),
                req.getCategory() != null ? req.getCategory() : "General Feedback",
                req.getMessage()
        );
        return ResponseEntity.ok().build();
    }
}
