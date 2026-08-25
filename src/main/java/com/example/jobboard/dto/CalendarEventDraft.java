package com.example.jobboard.dto;

import java.time.LocalDate;

/** What to write to a Google Calendar event. Either an all-day date, or a timed dateTime + IANA zone. */
public record CalendarEventDraft(
        String summary,
        String description,
        boolean allDay,
        LocalDate allDayDate,
        String startDateTime,
        String endDateTime,
        String timeZone
) {}
