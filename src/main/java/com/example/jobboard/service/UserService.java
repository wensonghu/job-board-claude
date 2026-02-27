package com.example.jobboard.service;

import com.example.jobboard.dto.RegistrationRequest;
import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.AuthProvider;
import com.example.jobboard.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    /**
     * Register a new LOCAL (email/password) user. Sends a verification email.
     * Throws IllegalArgumentException if email is already taken.
     */
    public AppUser register(RegistrationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setUserKey(sha256Hex(email));
        user.setDisplayName(request.getDisplayName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(true);

        AppUser saved = appUserRepository.save(user);
        logger.info("Registered new LOCAL user: {}", email);
        return saved;
    }

    /**
     * Verify email via token. Returns the activated user, or throws if token is invalid/expired.
     */
    public AppUser verifyEmail(String token) {
        AppUser user = appUserRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token."));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired. Please register again.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        AppUser saved = appUserRepository.save(user);
        logger.info("Email verified for user: {}", saved.getEmail());
        return saved;
    }

    /**
     * Resend verification email. Resets the token so the link is fresh.
     */
    public void resendVerification(String email) {
        AppUser user = appUserRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("No account found for that email."));
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("This email is already verified — please sign in.");
        }
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        appUserRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
        logger.info("Resent verification email to {}", email);
    }

    /**
     * Find or create an AppUser for a Google OAuth2 login.
     * Looks up by googleSub first, then by email (account linking).
     */
    public AppUser findOrCreateGoogleUser(String email, String googleSub, String displayName) {
        // Try by googleSub first
        return appUserRepository.findByGoogleSub(googleSub).orElseGet(() ->
            // Try by email (link existing LOCAL account)
            appUserRepository.findByEmail(email.toLowerCase().trim()).map(existing -> {
                if (existing.getGoogleSub() == null) {
                    existing.setGoogleSub(googleSub);
                    return appUserRepository.save(existing);
                }
                return existing;
            }).orElseGet(() -> {
                // Create new Google user
                AppUser user = new AppUser();
                user.setEmail(email.toLowerCase().trim());
                user.setUserKey(sha256Hex(email.toLowerCase().trim()));
                user.setDisplayName(displayName);
                user.setGoogleSub(googleSub);
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setEmailVerified(true);
                AppUser saved = appUserRepository.save(user);
                logger.info("Created new GOOGLE user: {}", email);
                return saved;
            })
        );
    }

    /**
     * Resolve AppUser from session attribute (set by OAuth2LoginSuccessHandler or form login).
     */
    public AppUser findById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public AppUser findByEmail(String email) {
        return appUserRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
