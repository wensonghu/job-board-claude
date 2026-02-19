package com.example.jobboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CardStatus {
    @JsonProperty("in-progress")
    IN_PROGRESS,
    @JsonProperty("interview-schedule-pending")
    INTERVIEW_SCHEDULE_PENDING,
    @JsonProperty("interview-date-confirmed")
    INTERVIEW_DATE_CONFIRMED,
    @JsonProperty("rejected")
    REJECTED
}