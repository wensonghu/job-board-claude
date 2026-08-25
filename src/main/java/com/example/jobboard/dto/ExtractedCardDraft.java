package com.example.jobboard.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Loosely-typed extraction result from pasted job text. All fields are nullable —
 * the model is instructed to leave anything it isn't confident about as null rather
 * than guess, since this only prefills a form for user review, never auto-saves.
 */
public record ExtractedCardDraft(
        @JsonPropertyDescription("Company name, or null if not mentioned")
        String company,

        @JsonPropertyDescription("Job title/position, or null if not mentioned")
        String position,

        @JsonPropertyDescription("One of: early, recruiter, hm, other, final — or null if unclear")
        String stage,

        @JsonPropertyDescription("One of: in-progress, interview-schedule-pending, interview-date-confirmed, "
                + "interview-completed, offer-pending, offer-received, offer-accepted, declined, rejected — or null if unclear")
        String status,

        @JsonPropertyDescription("Interview date as YYYY-MM-DD, or null if no interview date is mentioned")
        String interviewDate,

        @JsonPropertyDescription("Interview time as 24-hour HH:mm, or null if no time is mentioned")
        String interviewTime,

        @JsonPropertyDescription("One of: PT, MT, CT, ET, GMT, UTC — or null if no timezone is mentioned or inferable")
        String interviewTimezone,

        @JsonPropertyDescription("Date the user applied, as YYYY-MM-DD, or null if not mentioned")
        String appliedDate,

        @JsonPropertyDescription("Name of the person who referred the user, or null if not mentioned")
        String referredBy,

        @JsonPropertyDescription("Any other relevant free-text notes worth keeping (recruiter name, next steps, "
                + "salary info, etc.), 200 characters or fewer, or null")
        String details
) {}
