package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "broadcast_message")
public class BroadcastMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String content;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
