package com.example.jobboard.service;

import com.example.jobboard.model.Card;
import com.example.jobboard.model.CardHistory;
import com.example.jobboard.model.GoogleCalendarToken;
import com.example.jobboard.repository.CardHistoryRepository;
import com.example.jobboard.repository.CardRepository;
import com.example.jobboard.repository.GoogleCalendarTokenRepository;
import com.example.jobboard.util.InterviewDateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Poll direction: Google Calendar -> PitStop. Only ever touches events PitStop itself
 * created (matched via Card.googleEventId) — the user's other calendar events are never
 * read into a card, modified, or deleted.
 */
@Component
public class GoogleCalendarSyncJob {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarSyncJob.class);

    @Autowired
    private GoogleCalendarTokenRepository tokenRepository;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private GoogleCalendarClient calendarClient;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardHistoryRepository cardHistoryRepository;

    @Scheduled(fixedDelayString = "${google.calendar.poll-interval-ms:300000}")
    public void pollAllUsers() {
        List<GoogleCalendarToken> tokens = tokenRepository.findAll();
        for (GoogleCalendarToken token : tokens) {
            try {
                pollOneUser(token);
            } catch (Exception e) {
                logger.error("Calendar poll failed for userId={}: {}", token.getUserId(), e.getMessage());
            }
        }
    }

    private void pollOneUser(GoogleCalendarToken token) throws Exception {
        String accessToken = googleOAuthService.getValidAccessToken(token);

        GoogleCalendarClient.EventsPage page;
        try {
            page = calendarClient.listEventsSince(accessToken, token.getSyncToken());
        } catch (GoogleCalendarClient.SyncTokenExpiredException e) {
            logger.info("Sync token expired for userId={} — falling back to bounded resync", token.getUserId());
            token.setSyncToken(null);
            page = calendarClient.listEventsSince(accessToken, null);
        }

        Map<String, Card> byEventId = cardRepository.findByUserIdAndGoogleEventIdIsNotNull(token.getUserId())
                .stream().collect(Collectors.toMap(Card::getGoogleEventId, c -> c));

        for (GoogleCalendarClient.RawEvent event : page.events()) {
            Card card = byEventId.get(event.id());
            if (card == null) continue; // not one of ours — never touch the user's other calendar events

            if ("cancelled".equals(event.status())) {
                card.setInterviewDate("TBD");
                card.setGoogleEventId(null);
                cardRepository.save(card);
                cardHistoryRepository.save(CardHistory.fromCard(card));
                continue;
            }

            String newInterviewDate = InterviewDateMapper.fromCalendarEvent(event);
            if (!Objects.equals(newInterviewDate, card.getInterviewDate())) {
                card.setInterviewDate(newInterviewDate);
                cardRepository.save(card);
                // Direct save, not CardService.updateCard — avoids re-triggering the push direction (would create a sync loop).
                cardHistoryRepository.save(CardHistory.fromCard(card));
            }
        }

        token.setSyncToken(page.nextSyncToken());
        token.setLastSyncedAt(Instant.now());
        tokenRepository.save(token);
    }
}
