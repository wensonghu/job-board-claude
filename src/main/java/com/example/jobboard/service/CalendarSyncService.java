package com.example.jobboard.service;

import com.example.jobboard.dto.CalendarEventDraft;
import com.example.jobboard.model.Card;
import com.example.jobboard.model.GoogleCalendarToken;
import com.example.jobboard.repository.CardRepository;
import com.example.jobboard.repository.GoogleCalendarTokenRepository;
import com.example.jobboard.util.InterviewDateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Push direction: PitStop -> Google Calendar. No-ops silently if the user hasn't connected
 * Calendar, and never lets a Calendar failure block saving a card.
 */
@Service
public class CalendarSyncService {

    private static final Logger logger = LoggerFactory.getLogger(CalendarSyncService.class);

    @Autowired
    private GoogleCalendarTokenRepository tokenRepository;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private GoogleCalendarClient calendarClient;

    @Autowired
    private CardRepository cardRepository;

    public void syncCardToCalendar(Card card) {
        try {
            GoogleCalendarToken token = tokenRepository.findByUserId(card.getUserId()).orElse(null);
            if (token == null) return;

            String accessToken = googleOAuthService.getValidAccessToken(token);
            Optional<CalendarEventDraft> draft = InterviewDateMapper.toCalendarEvent(card);

            if (draft.isPresent() && card.getGoogleEventId() == null) {
                String eventId = calendarClient.createEvent(accessToken, draft.get());
                card.setGoogleEventId(eventId);
                cardRepository.save(card);
            } else if (draft.isPresent()) {
                calendarClient.updateEvent(accessToken, card.getGoogleEventId(), draft.get());
            } else if (card.getGoogleEventId() != null) {
                calendarClient.deleteEvent(accessToken, card.getGoogleEventId());
                card.setGoogleEventId(null);
                cardRepository.save(card);
            }
        } catch (Exception e) {
            logger.error("Calendar sync failed for card {}: {}", card.getId(), e.getMessage());
        }
    }

    public void deleteCardEvent(Card card) {
        if (card.getGoogleEventId() == null) return;
        try {
            GoogleCalendarToken token = tokenRepository.findByUserId(card.getUserId()).orElse(null);
            if (token == null) return;
            String accessToken = googleOAuthService.getValidAccessToken(token);
            calendarClient.deleteEvent(accessToken, card.getGoogleEventId());
        } catch (Exception e) {
            logger.error("Calendar event delete failed for card {}: {}", card.getId(), e.getMessage());
        }
    }
}
