package com.example.jobboard.dto;

/** A Calendar event not yet linked to any PitStop card, offered up for import review. */
public record UnmatchedCalendarEvent(String googleEventId, String summary, String description, String interviewDate) {}
