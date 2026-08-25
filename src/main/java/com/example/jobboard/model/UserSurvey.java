package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_survey")
public class UserSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_search_stage", length = 60)
    private String jobSearchStage;

    @Column(name = "search_duration", length = 30)
    private String searchDuration;

    @Column(name = "support_need", length = 40)
    private String supportNeed;

    @Column(name = "find_helpful", length = 3)
    private String findHelpful;

    @Column(name = "helpful_reason", columnDefinition = "TEXT")
    private String helpfulReason;

    @Column(length = 254)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId()               { return id; }
    public String getJobSearchStage() { return jobSearchStage; }
    public String getSearchDuration() { return searchDuration; }
    public String getSupportNeed()    { return supportNeed; }
    public String getFindHelpful()    { return findHelpful; }
    public String getHelpfulReason()  { return helpfulReason; }
    public String getEmail()          { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setJobSearchStage(String v) { this.jobSearchStage = v; }
    public void setSearchDuration(String v) { this.searchDuration = v; }
    public void setSupportNeed(String v)    { this.supportNeed = v; }
    public void setFindHelpful(String v)    { this.findHelpful = v; }
    public void setHelpfulReason(String v)  { this.helpfulReason = v; }
    public void setEmail(String v)          { this.email = v; }
}
