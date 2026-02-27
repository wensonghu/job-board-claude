package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alert_dismissal")
public class AlertDismissal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "alert_key", nullable = false, length = 100)
    private String alertKey;

    @Column(name = "dismissed_at", nullable = false, updatable = false)
    private OffsetDateTime dismissedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }

    public OffsetDateTime getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(OffsetDateTime dismissedAt) { this.dismissedAt = dismissedAt; }
}
