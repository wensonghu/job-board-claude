package com.example.jobboard.dto;

import com.example.jobboard.model.CardStage;
import com.example.jobboard.model.CardStatus;

/** Fields for creating a card from a reviewed, user-confirmed Calendar event import. */
public record ImportCalendarEventRequest(
        String googleEventId,
        String company,
        String position,
        CardStage stage,
        CardStatus status,
        String interviewDate,
        String appliedDate,
        String referredBy,
        String details
) {}
