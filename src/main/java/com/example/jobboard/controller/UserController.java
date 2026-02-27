package com.example.jobboard.controller;

import com.example.jobboard.dto.RegistrationRequest;
import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegistrationRequest request) {
        try {
            userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Account created. You can now sign in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed. Please try again."));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }
        try {
            userService.resendVerification(email.trim());
            return ResponseEntity.ok(Map.of("message", "Verification email sent. Please check your inbox."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send email. Please try again."));
        }
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        try {
            AppUser user = userService.verifyEmail(token);
            // Log the user in by setting session attribute; they can now sign in
            request.getSession().setAttribute("appUserId", user.getId());
            response.sendRedirect("/?verified=true");
        } catch (IllegalArgumentException e) {
            response.sendRedirect("/?verify_error=true");
        }
    }
}
