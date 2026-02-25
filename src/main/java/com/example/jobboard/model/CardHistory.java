package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Audit log: one row per card change (create, update, delete).
 * card_id  — references card.id for app-originated changes (null for sheet migration rows)
 * sheet_id — original Google Sheets ID (null for app-originated rows)
 * is_deleted = true marks deletion events; the card state at time of deletion is preserved.
 */
@Entity
@Table(name = "card_history")
public class CardHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "sheet_id")
    private Long sheetId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    private String company;
    private String position;

    @Enumerated(EnumType.STRING)
    private CardStage stage;

    private LocalDate date;

    @Column(name = "interview_date")
    private String interviewDate;

    @Column(name = "referred_by")
    private String referredBy;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(length = 255)
    private String details;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = Instant.now();
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static CardHistory fromCard(Card card) {
        CardHistory h = new CardHistory();
        h.cardId       = card.getId();
        h.userId       = card.getUserId();
        h.company      = card.getCompany();
        h.position     = card.getPosition();
        h.stage        = card.getStage();
        h.date         = card.getDate();
        h.interviewDate = card.getInterviewDate();
        h.referredBy   = card.getReferredBy();
        h.status       = card.getStatus();
        h.details      = card.getDetails();
        h.isDeleted    = false;
        return h;
    }

    public static CardHistory deletionOf(Card card) {
        CardHistory h = fromCard(card);
        h.isDeleted = true;
        return h;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()            { return id; }
    public Long getCardId()        { return cardId; }
    public Long getSheetId()       { return sheetId; }
    public Long getUserId()        { return userId; }
    public Instant getChangedAt()  { return changedAt; }
    public String getCompany()     { return company; }
    public String getPosition()    { return position; }
    public CardStage getStage()    { return stage; }
    public LocalDate getDate()     { return date; }
    public String getInterviewDate() { return interviewDate; }
    public String getReferredBy()  { return referredBy; }
    public CardStatus getStatus()  { return status; }
    public String getDetails()     { return details; }
    public boolean isDeleted()     { return isDeleted; }

    // ── Setters (needed for migration rows) ───────────────────────────────────

    public void setSheetId(Long sheetId)       { this.sheetId = sheetId; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }
}
