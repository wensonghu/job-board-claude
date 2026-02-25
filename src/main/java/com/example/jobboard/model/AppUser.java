package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // SHA-256 of lowercase email — stable external identifier
    @Column(name = "user_key", unique = true, nullable = false)
    private String userKey;

    private String displayName;

    // Null for Google-only users
    private String passwordHash;

    // Null for LOCAL users
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;

    @Column(nullable = false)
    private boolean emailVerified = false;

    private String verificationToken;

    private LocalDateTime verificationTokenExpiry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String role = "BASIC";

    @Column(name = "last_seen_broadcast_id")
    private Long lastSeenBroadcastId;

    @Column(name = "user_type", nullable = false)
    private String userType = "BETA";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public LocalDateTime getVerificationTokenExpiry() { return verificationTokenExpiry; }
    public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) { this.verificationTokenExpiry = verificationTokenExpiry; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getLastSeenBroadcastId() { return lastSeenBroadcastId; }
    public void setLastSeenBroadcastId(Long lastSeenBroadcastId) { this.lastSeenBroadcastId = lastSeenBroadcastId; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
