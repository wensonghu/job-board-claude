package com.example.jobboard.util;

import com.example.jobboard.dto.CalendarEventDraft;
import com.example.jobboard.model.Card;
import com.example.jobboard.service.GoogleCalendarClient.RawEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps Card.interviewDate's pipe-delimited abbreviated-timezone format
 * ("YYYY-MM-DD|HH:mm|TZ" or "TBD") to/from Google Calendar's IANA-zone event fields.
 */
public final class InterviewDateMapper {

    private static final Logger logger = LoggerFactory.getLogger(InterviewDateMapper.class);

    public static final Map<String, String> TZ_ABBR_TO_IANA = new LinkedHashMap<>();
    private static final Map<String, String> IANA_TO_TZ_ABBR = new LinkedHashMap<>();

    static {
        TZ_ABBR_TO_IANA.put("PT", "America/Los_Angeles");
        TZ_ABBR_TO_IANA.put("MT", "America/Denver");
        TZ_ABBR_TO_IANA.put("CT", "America/Chicago");
        TZ_ABBR_TO_IANA.put("ET", "America/New_York");
        TZ_ABBR_TO_IANA.put("GMT", "Etc/GMT");
        TZ_ABBR_TO_IANA.put("UTC", "UTC");
        TZ_ABBR_TO_IANA.forEach((abbr, iana) -> IANA_TO_TZ_ABBR.put(iana, abbr));
    }

    private InterviewDateMapper() {}

    public static Optional<CalendarEventDraft> toCalendarEvent(Card card) {
        String interviewDate = card.getInterviewDate();
        if (interviewDate == null || interviewDate.isBlank() || "TBD".equalsIgnoreCase(interviewDate)) {
            return Optional.empty();
        }

        String[] parts = interviewDate.split("\\|");
        String datePart = parts[0];
        String timePart = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : null;
        String tzAbbr = (parts.length > 2 && !parts[2].isBlank()) ? parts[2] : null;

        LocalDate date;
        try {
            date = LocalDate.parse(datePart);
        } catch (DateTimeParseException e) {
            logger.warn("Card {} has unparseable interviewDate '{}' — skipping calendar sync", card.getId(), interviewDate);
            return Optional.empty();
        }

        String summary = buildSummary(card);

        if (timePart == null) {
            return Optional.of(new CalendarEventDraft(summary, buildDescription(card), true, date, null, null, null));
        }

        LocalTime time;
        try {
            time = LocalTime.parse(timePart);
        } catch (DateTimeParseException e) {
            logger.warn("Card {} has unparseable interview time '{}' — treating as all-day", card.getId(), timePart);
            return Optional.of(new CalendarEventDraft(summary, buildDescription(card), true, date, null, null, null));
        }

        String ianaZone = tzAbbr != null ? TZ_ABBR_TO_IANA.get(tzAbbr) : null;
        if (ianaZone == null) {
            if (tzAbbr != null) {
                logger.warn("Card {} has unrecognized timezone '{}' — falling back to UTC", card.getId(), tzAbbr);
            }
            ianaZone = "UTC";
        }

        String start = date.atTime(time).toString();
        String end = date.atTime(time).plusHours(1).toString();
        return Optional.of(new CalendarEventDraft(summary, buildDescription(card), false, null, start, end, ianaZone));
    }

    /** Reverse direction: derive the card's interviewDate string from a Calendar event the poll job read back. */
    public static String fromCalendarEvent(RawEvent event) {
        if (event.startDate() != null) {
            return event.startDate();
        }
        if (event.startDateTime() == null) {
            return "TBD";
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(event.startDateTime());
            String tzAbbr = event.startTimeZone() != null ? IANA_TO_TZ_ABBR.get(event.startTimeZone()) : null;
            if (tzAbbr == null) {
                logger.warn("Calendar event {} has unmappable timezone '{}' — falling back to UTC",
                        event.id(), event.startTimeZone());
                tzAbbr = "UTC";
            }
            return odt.toLocalDate() + "|" + String.format("%02d:%02d", odt.getHour(), odt.getMinute()) + "|" + tzAbbr;
        } catch (DateTimeParseException e) {
            logger.warn("Calendar event {} has unparseable dateTime '{}'", event.id(), event.startDateTime());
            return "TBD";
        }
    }

    private static String buildSummary(Card card) {
        String company = card.getCompany() != null ? card.getCompany() : "Interview";
        String position = card.getPosition() != null ? card.getPosition() : "";
        return position.isBlank() ? company + " interview" : company + " — " + position + " interview";
    }

    private static String buildDescription(Card card) {
        return "Synced from PitStop" + (card.getDetails() != null && !card.getDetails().isBlank()
                ? "\n\n" + card.getDetails() : "");
    }
}
