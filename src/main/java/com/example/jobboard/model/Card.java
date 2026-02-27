package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String company;
    private String position;

    @Enumerated(EnumType.STRING)
    private CardStage stage;

    private LocalDate date; // Corresponds to last action date

    @Column(name = "applied_date")
    private LocalDate appliedDate;

    private String interviewDate; // Format: "YYYY-MM-DD|HH:mm|TZ" or "TBD"

    private String referredBy;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(length = 255) // Increased length from default
    private String details;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public CardStage getStage() { return stage; }
    public void setStage(CardStage stage) { this.stage = stage; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }

    public String getInterviewDate() { return interviewDate; }
    public void setInterviewDate(String interviewDate) { this.interviewDate = interviewDate; }

    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }

    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Transient
    public String getColor() {
        // 1. Grey: Rejected OR 2 weeks since last action
        if (CardStatus.REJECTED.equals(this.status)) {
            return "grey";
        }
        if (this.date != null && this.date.isBefore(LocalDate.now().minusWeeks(2))) {
            return "grey";
        }

        // 2. Yellow: Interview within next 48 hours
        if (this.interviewDate != null && !this.interviewDate.isEmpty() && !"TBD".equalsIgnoreCase(this.interviewDate)) {
            try {
                String[] parts = this.interviewDate.split("\\|");
                String datePart = parts[0];
                String timePart = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : "00:00";

                LocalDateTime interviewDateTime = LocalDateTime.parse(datePart + "T" + timePart);
                LocalDateTime now = LocalDateTime.now();

                if (interviewDateTime.isAfter(now) && interviewDateTime.isBefore(now.plusHours(48))) {
                    return "yellow";
                }
            } catch (Exception e) {
                // Ignore parse errors, fall through to white
            }
        }

        // 3. White: Default (includes passed interviews)
        return "white";
    }
}