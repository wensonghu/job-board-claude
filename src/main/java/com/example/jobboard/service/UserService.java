package com.example.jobboard.service;

import com.example.jobboard.dto.RegistrationRequest;
import com.example.jobboard.model.AppUser;
import com.example.jobboard.model.AuthProvider;
import com.example.jobboard.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        user.setStatus("REGISTERED");

        AppUser saved = appUserRepository.save(user);
        logger.info("Registered new LOCAL user: {}", email);
        emailService.sendNewUserNotification(email, request.getDisplayName());
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
                user.setStatus("REGISTERED");
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

    /**
     * Look up or create a PENDING (guest) user by session token.
     * Idempotent — safe to call on every page load.
     */
    public synchronized AppUser initGuestSession(String sessionToken) {
        return appUserRepository.findBySessionToken(sessionToken).orElseGet(() -> {
            String email = "pending_" + UUID.randomUUID() + "@pitstop.local";
            AppUser guest = new AppUser();
            guest.setEmail(email);
            guest.setUserKey(sha256Hex(email));
            guest.setDisplayName("Guest");
            guest.setAuthProvider(AuthProvider.LOCAL);
            guest.setEmailVerified(false);
            guest.setStatus("PENDING");
            guest.setSessionToken(sessionToken);
            AppUser saved = appUserRepository.save(guest);
            logger.info("Created PENDING guest user id={}", saved.getId());
            return saved;
        });
    }

    /**
     * Convert a PENDING user to a REGISTERED local account.
     * If the real email conflicts with an existing REGISTERED user, merge into that account.
     */
    @Transactional
    public AppUser convertPendingUserToLocal(AppUser pending, String email, String password, String displayName) {
        email = email.toLowerCase().trim();
        AppUser existing = appUserRepository.findByEmail(email).orElse(null);
        if (existing != null && "REGISTERED".equals(existing.getStatus())) {
            throw new IllegalArgumentException("An account with this email already exists. Please sign in.");
        }
        // No conflict: update the PENDING user row in place
        pending.setEmail(email);
        pending.setUserKey(sha256Hex(email));
        pending.setDisplayName(displayName);
        pending.setPasswordHash(passwordEncoder.encode(password));
        pending.setAuthProvider(AuthProvider.LOCAL);
        pending.setEmailVerified(true);
        pending.setStatus("REGISTERED");
        pending.setSessionToken(null);
        AppUser saved = appUserRepository.save(pending);
        logger.info("Converted PENDING user {} to REGISTERED (email: {})", saved.getId(), email);
        emailService.sendNewUserNotification(email, displayName);
        return saved;
    }

    /**
     * Convert a PENDING user to a REGISTERED Google account.
     * If the Google sub or email matches an existing REGISTERED user, merge into that account.
     */
    @Transactional
    public AppUser convertPendingUserViaGoogle(AppUser pending, String email, String googleSub, String displayName) {
        email = email.toLowerCase().trim();
        // Check by googleSub first
        AppUser bySub = appUserRepository.findByGoogleSub(googleSub).orElse(null);
        if (bySub != null && "REGISTERED".equals(bySub.getStatus())) {
            mergePendingIntoExisting(pending, bySub);
            logger.info("Merged PENDING user {} into existing Google user {} (sub match)", pending.getId(), bySub.getId());
            return bySub;
        }
        // Check by email
        AppUser byEmail = appUserRepository.findByEmail(email).orElse(null);
        if (byEmail != null && "REGISTERED".equals(byEmail.getStatus())) {
            if (byEmail.getGoogleSub() == null) {
                byEmail.setGoogleSub(googleSub);
                appUserRepository.save(byEmail);
            }
            mergePendingIntoExisting(pending, byEmail);
            logger.info("Merged PENDING user {} into existing user {} (email match)", pending.getId(), byEmail.getId());
            return byEmail;
        }
        // No conflict: update the PENDING user row in place
        pending.setEmail(email);
        pending.setUserKey(sha256Hex(email));
        pending.setDisplayName(displayName);
        pending.setGoogleSub(googleSub);
        pending.setAuthProvider(AuthProvider.GOOGLE);
        pending.setEmailVerified(true);
        pending.setStatus("REGISTERED");
        pending.setSessionToken(null);
        AppUser saved = appUserRepository.save(pending);
        logger.info("Converted PENDING user {} to REGISTERED (Google, email: {})", saved.getId(), email);
        return saved;
    }

    /**
     * Transfer all data from a PENDING user to an existing REGISTERED user, then delete the PENDING user.
     */
    @Transactional
    public void mergePendingIntoExisting(AppUser pending, AppUser existing) {
        jdbcTemplate.update("UPDATE card SET user_id = ? WHERE user_id = ?", existing.getId(), pending.getId());
        jdbcTemplate.update("UPDATE card_history SET user_id = ? WHERE user_id = ?", existing.getId(), pending.getId());
        // alert_dismissal has a unique constraint on (user_id, alert_key) — delete PENDING's to avoid conflicts
        jdbcTemplate.update("DELETE FROM alert_dismissal WHERE user_id = ?", pending.getId());
        appUserRepository.delete(pending);
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
