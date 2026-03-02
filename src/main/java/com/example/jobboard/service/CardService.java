package com.example.jobboard.service;

import com.example.jobboard.model.Card;
import com.example.jobboard.model.CardHistory;
import com.example.jobboard.model.CardStage;
import com.example.jobboard.model.CardStatus;
import com.example.jobboard.repository.CardHistoryRepository;
import com.example.jobboard.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        existing.setAppliedDate(cardDetails.getAppliedDate());
        existing.setInterviewDate(cardDetails.getInterviewDate());
        existing.setReferredBy(cardDetails.getReferredBy());
        existing.setDetails(cardDetails.getDetails());
        existing.setStage(cardDetails.getStage());
        existing.setStatus(cardDetails.getStatus());
        applyInterviewStatusRule(existing);
        Card saved = cardRepository.save(existing);
        cardHistoryRepository.save(CardHistory.fromCard(saved));

        // Auto-create offer card when interview is completed at the Final stage
        if (saved.getStatus() == CardStatus.INTERVIEW_COMPLETED
                && saved.getStage() == CardStage.FINAL
                && !cardRepository.existsByUserIdAndCompanyAndPositionAndStatus(
                        userId, saved.getCompany(), saved.getPosition(), CardStatus.OFFER_PENDING)) {
            Card offerCard = new Card();
            offerCard.setUserId(userId);
            offerCard.setCompany(saved.getCompany());
            offerCard.setPosition(saved.getPosition());
            offerCard.setStage(CardStage.FINAL);
            offerCard.setStatus(CardStatus.OFFER_PENDING);
            offerCard.setDate(LocalDate.now());
            offerCard.setAppliedDate(saved.getAppliedDate());
            offerCard.setReferredBy(saved.getReferredBy());
            Card savedOffer = cardRepository.save(offerCard);
            cardHistoryRepository.save(CardHistory.fromCard(savedOffer));
            logger.info("Auto-created offer card {} for userId={} company={}", savedOffer.getId(), userId, saved.getCompany());
        }

        return saved;
    }

    /** Stage-only update — used by drag-and-drop. Does not trigger status rules. */
    @Transactional
    public Card updateStage(Long id, String stageName, Long userId) {
        Card existing = cardRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + id));
        CardStage newStage = parseStage(stageName);
        existing.setStage(newStage);
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

    private CardStage parseStage(String s) {
        for (CardStage cs : CardStage.values()) {
            if (cs.name().equalsIgnoreCase(s.replace("-", "_"))) return cs;
        }
        throw new IllegalArgumentException("Unknown stage: " + s);
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
