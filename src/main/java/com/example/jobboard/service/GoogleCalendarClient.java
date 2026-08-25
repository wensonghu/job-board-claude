package com.example.jobboard.service;

import com.example.jobboard.dto.CalendarEventDraft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/** Raw REST wrapper around Google Calendar v3 — this codebase's existing external-API pattern (see EmailService). */
@Service
public class GoogleCalendarClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarClient.class);
    private static final String EVENTS_BASE = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class SyncTokenExpiredException extends RuntimeException {}

    /** The access token was rejected — distinct from other failures so callers can refresh and retry once. */
    public static class GoogleAuthException extends RuntimeException {
        public GoogleAuthException(String message) { super(message); }
    }

    public record RawEvent(String id, String status, String startDate, String startDateTime, String startTimeZone) {}

    public record EventsPage(List<RawEvent> events, String nextSyncToken) {}

    public String createEvent(String accessToken, CalendarEventDraft draft) throws IOException, InterruptedException {
        String body = toJson(draft);
        HttpRequest request = authedRequest(accessToken, EVENTS_BASE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new GoogleAuthException("Calendar createEvent: " + response.body());
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Calendar createEvent failed: " + response.statusCode() + " " + response.body());
        }
        return objectMapper.readTree(response.body()).path("id").asText(null);
    }

    public void updateEvent(String accessToken, String eventId, CalendarEventDraft draft) throws IOException, InterruptedException {
        String body = toJson(draft);
        HttpRequest request = authedRequest(accessToken, EVENTS_BASE + "/" + eventId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new GoogleAuthException("Calendar updateEvent: " + response.body());
        }
        if (response.statusCode() == 404 || response.statusCode() == 410) {
            logger.info("Calendar event {} already gone on update — ignoring", eventId);
            return;
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Calendar updateEvent failed: " + response.statusCode() + " " + response.body());
        }
    }

    public void deleteEvent(String accessToken, String eventId) throws IOException, InterruptedException {
        HttpRequest request = authedRequest(accessToken, EVENTS_BASE + "/" + eventId)
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new GoogleAuthException("Calendar deleteEvent: " + response.body());
        }
        if (response.statusCode() == 404 || response.statusCode() == 410) {
            return; // already gone — not an error
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Calendar deleteEvent failed: " + response.statusCode() + " " + response.body());
        }
    }

    /** Pass syncToken=null for a bounded initial sync (last 90 days forward); pass a stored token for incremental sync. */
    public EventsPage listEventsSince(String accessToken, String syncToken) throws IOException, InterruptedException {
        String url = EVENTS_BASE + "?singleEvents=true";
        if (syncToken != null) {
            url += "&syncToken=" + syncToken;
        } else {
            url += "&timeMin=" + java.time.Instant.now().minus(java.time.Duration.ofDays(90))
                    .toString();
        }
        HttpRequest request = authedRequest(accessToken, url).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new GoogleAuthException("Calendar listEvents: " + response.body());
        }
        if (response.statusCode() == 410) {
            throw new SyncTokenExpiredException();
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Calendar listEvents failed: " + response.statusCode() + " " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        List<RawEvent> events = new ArrayList<>();
        for (JsonNode item : json.path("items")) {
            JsonNode start = item.path("start");
            events.add(new RawEvent(
                    item.path("id").asText(null),
                    item.path("status").asText(null),
                    start.hasNonNull("date") ? start.get("date").asText() : null,
                    start.hasNonNull("dateTime") ? start.get("dateTime").asText() : null,
                    start.hasNonNull("timeZone") ? start.get("timeZone").asText() : null
            ));
        }
        String nextSyncToken = json.hasNonNull("nextSyncToken") ? json.get("nextSyncToken").asText() : null;
        return new EventsPage(events, nextSyncToken);
    }

    private HttpRequest.Builder authedRequest(String accessToken, String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken);
    }

    private String toJson(CalendarEventDraft draft) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("summary", draft.summary());
        if (draft.description() != null) root.put("description", draft.description());

        ObjectNode start = objectMapper.createObjectNode();
        ObjectNode end = objectMapper.createObjectNode();
        if (draft.allDay()) {
            start.put("date", draft.allDayDate().toString());
            end.put("date", draft.allDayDate().plusDays(1).toString());
        } else {
            start.put("dateTime", draft.startDateTime());
            start.put("timeZone", draft.timeZone());
            end.put("dateTime", draft.endDateTime());
            end.put("timeZone", draft.timeZone());
        }
        root.set("start", start);
        root.set("end", end);
        return root.toString();
    }
}
