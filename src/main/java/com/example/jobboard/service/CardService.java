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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (card.getDate() == null) card.setDate(LocalDate.now());
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
        existing.setDate(cardDetails.getDate() != null ? cardDetails.getDate() : LocalDate.now());
        existing.setAppliedDate(cardDetails.getAppliedDate());
        existing.setInterviewDate(cardDetails.getInterviewDate());
        existing.setReferredBy(cardDetails.getReferredBy());
        existing.setDetails(cardDetails.getDetails());
        existing.setStage(cardDetails.getStage());
        existing.setStatus(cardDetails.getStatus());
        applyInterviewStatusRule(existing);
        Card saved = cardRepository.save(existing);
        cardHistoryRepository.save(CardHistory.fromCard(saved));

        // Cascade rejection: reject all sibling cards with same company + position
        if (saved.getStatus() == CardStatus.REJECTED
                && saved.getCompany() != null && saved.getPosition() != null) {
            List<Card> siblings = cardRepository.findByUserIdAndCompanyAndPosition(
                    userId, saved.getCompany(), saved.getPosition());
            for (Card sibling : siblings) {
                if (!sibling.getId().equals(saved.getId())
                        && sibling.getStatus() != CardStatus.REJECTED) {
                    sibling.setStatus(CardStatus.REJECTED);
                    Card rejectedSibling = cardRepository.save(sibling);
                    cardHistoryRepository.save(CardHistory.fromCard(rejectedSibling));
                    logger.info("Cascade-rejected card {} (company={} position={}) for userId={}",
                            sibling.getId(), saved.getCompany(), saved.getPosition(), userId);
                }
            }
        }

        // Auto-create offer card when interview is completed at the Final stage
        if (saved.getStatus() == CardStatus.INTERVIEW_COMPLETED
                && saved.getStage() == CardStage.FINAL) {
            String markedCompany = (saved.getCompany() != null ? saved.getCompany() : "") + " [Offer]";
            if (!cardRepository.existsByUserIdAndCompanyAndStatus(userId, markedCompany, CardStatus.OFFER_PENDING)) {
                Card offerCard = new Card();
                offerCard.setUserId(userId);
                offerCard.setCompany(markedCompany);
                offerCard.setPosition(saved.getPosition());
                offerCard.setStage(CardStage.FINAL);
                offerCard.setStatus(CardStatus.OFFER_PENDING);
                offerCard.setDate(LocalDate.now());
                offerCard.setAppliedDate(saved.getAppliedDate());
                offerCard.setReferredBy(saved.getReferredBy());
                Card savedOffer = cardRepository.save(offerCard);
                cardHistoryRepository.save(CardHistory.fromCard(savedOffer));
                logger.info("Auto-created offer card {} for userId={} company={}", savedOffer.getId(), userId, markedCompany);
            }
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
        existing.setDate(LocalDate.now());
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

    /**
     * Per-company pipeline: days from first INTERVIEW_DATE_CONFIRMED to last
     * INTERVIEW_COMPLETED (or elapsed to now if still in progress).
     * Entries whose company name starts with "test" (case-insensitive) are excluded.
     */
    /**
     * Returns per-company min/max action date from card_history.
     * "[Offer]" suffix is stripped so offer cards merge with their parent company.
     * Only companies with at least one RECRUITER/HM/OTHER/FINAL stage record are included.
     */
    public List<Map<String, Object>> getPipelineSummary(Long userId) {
        List<CardHistory> history = cardHistoryRepository.findByUserIdOrderByChangedAtDesc(userId);

        Map<String, LocalDate> minDate = new LinkedHashMap<>();
        Map<String, LocalDate> maxDate = new LinkedHashMap<>();
        Set<String> advancedCompanies = new HashSet<>();

        for (CardHistory h : history) {
            if (h.isDeleted() || h.getDate() == null || h.getCompany() == null) continue;
            String raw = h.getCompany();
            String key = raw.endsWith(" [Offer]") ? raw.substring(0, raw.length() - 8).trim() : raw;
            if (isTestCompany(key)) continue;

            minDate.merge(key, h.getDate(), (a, b) -> b.isBefore(a) ? b : a);
            maxDate.merge(key, h.getDate(), (a, b) -> b.isAfter(a) ? b : a);

            if (h.getStage() == CardStage.RECRUITER
                    || h.getStage() == CardStage.HM
                    || h.getStage() == CardStage.OTHER
                    || h.getStage() == CardStage.FINAL) {
                advancedCompanies.add(key);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String company : minDate.keySet()) {
            if (!advancedCompanies.contains(company)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("company", company);
            row.put("firstAction", minDate.get(company).toString());
            row.put("lastAction",  maxDate.get(company).toString());
            result.add(row);
        }
        return result;
    }

    private String pkey(String company) {
        return company != null ? company : "";
    }

    private boolean isTestCompany(String companyKey) {
        return companyKey.toLowerCase().startsWith("test");
    }

    /** Returns deduplicated completed interview events from history, for the calendar. */
    public List<Map<String, Object>> getInterviewHistory(Long userId) {
        List<com.example.jobboard.model.CardHistory> raw =
                cardHistoryRepository.findByUserIdAndInterviewDateIsNotNull(userId);
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (com.example.jobboard.model.CardHistory h : raw) {
            String idate = h.getInterviewDate();
            if (idate == null || idate.isBlank() || "TBD".equalsIgnoreCase(idate)) continue;
            // Only count interviews whose date has passed (date is first segment before |)
            String datePart = idate.split("\\|")[0];
            try {
                if (LocalDate.parse(datePart).isAfter(today)) continue;
            } catch (Exception e) { continue; }
            String key = h.getCardId() + "|" + datePart;
            if (seen.add(key)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("cardId",        h.getCardId());
                entry.put("company",       h.getCompany());
                entry.put("position",      h.getPosition());
                entry.put("interviewDate", idate);
                entry.put("stage", h.getStage() != null ? h.getStage().name().toLowerCase().replace('_', '-') : null);
                result.add(entry);
            }
        }
        return result;
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
