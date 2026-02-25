package com.example.jobboard.service;

import com.example.jobboard.model.Card;
import com.example.jobboard.model.CardHistory;
import com.example.jobboard.model.CardStatus;
import com.example.jobboard.repository.CardHistoryRepository;
import com.example.jobboard.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardHistoryRepository cardHistoryRepository;

    public List<Card> getAllCards(Long userId) {
        logger.info("Fetching cards for userId={}", userId);
        return cardRepository.findByUserId(userId);
    }

    @Transactional
    public Card createCard(Card card, Long userId) {
        card.setUserId(userId);
        applyInterviewStatusRule(card);
        Card saved = cardRepository.save(card);
        cardHistoryRepository.save(CardHistory.fromCard(saved));
        return saved;
    }

    @Transactional
    public Card updateCard(Long id, Card cardDetails, Long userId) {
        Card existing = cardRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + id));
        existing.setCompany(cardDetails.getCompany());
        existing.setPosition(cardDetails.getPosition());
        existing.setDate(cardDetails.getDate());
        existing.setInterviewDate(cardDetails.getInterviewDate());
        existing.setReferredBy(cardDetails.getReferredBy());
        existing.setDetails(cardDetails.getDetails());
        existing.setStage(cardDetails.getStage());
        existing.setStatus(cardDetails.getStatus());
        applyInterviewStatusRule(existing);
        Card saved = cardRepository.save(existing);
        cardHistoryRepository.save(CardHistory.fromCard(saved));
        return saved;
    }

    @Transactional
    public void deleteCard(Long id, Long userId) {
        Card card = cardRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + id));
        cardHistoryRepository.save(CardHistory.deletionOf(card));
        cardRepository.delete(card);
    }

    private void applyInterviewStatusRule(Card card) {
        String interviewDate = card.getInterviewDate();
        if (interviewDate != null && !interviewDate.isEmpty() && !"TBD".equalsIgnoreCase(interviewDate)) {
            if (card.getStatus() == null
                    || card.getStatus() == CardStatus.IN_PROGRESS
                    || card.getStatus() == CardStatus.INTERVIEW_SCHEDULE_PENDING) {
                card.setStatus(CardStatus.INTERVIEW_DATE_CONFIRMED);
            }
        }
    }
}
