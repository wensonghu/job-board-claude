package com.example.jobboard.service;

import com.example.jobboard.model.Card;
import com.example.jobboard.model.CardStatus;
import com.example.jobboard.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired(required = false)
    private GoogleSheetsService googleSheetsService;

    @Autowired(required = false)
    private OAuth2AuthorizedClientService authorizedClientService;

    private boolean isGoogleConnected(OAuth2AuthenticationToken authentication) {
        return authentication != null && googleSheetsService != null;
    }

    private OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken authentication) {
        if (authentication == null) return null;
        return this.authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );
    }

    public List<Card> getAllCards(OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        if (isGoogleConnected(authentication)) {
            return googleSheetsService.getAllCards(getAuthorizedClient(authentication));
        }
        return cardRepository.findAll();
    }

    public Card createCard(Card card, OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        applyInterviewStatusRule(card);
        if (isGoogleConnected(authentication)) {
            if (card.getId() == null) {
                card.setId(System.currentTimeMillis());
            }
            googleSheetsService.saveCard(getAuthorizedClient(authentication), card, false);
            return card;
        }
        return cardRepository.save(card);
    }

    public Card updateCard(Long id, Card cardDetails, OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        applyInterviewStatusRule(cardDetails);
        if (isGoogleConnected(authentication)) {
            cardDetails.setId(id);
            googleSheetsService.saveCard(getAuthorizedClient(authentication), cardDetails, false);
            return cardDetails;
        }
        return cardRepository.findById(id)
                .map(existingCard -> {
                    existingCard.setCompany(cardDetails.getCompany());
                    existingCard.setPosition(cardDetails.getPosition());
                    existingCard.setDate(cardDetails.getDate());
                    existingCard.setInterviewDate(cardDetails.getInterviewDate());
                    existingCard.setReferredBy(cardDetails.getReferredBy());
                    existingCard.setDetails(cardDetails.getDetails());
                    existingCard.setStage(cardDetails.getStage());
                    existingCard.setStatus(cardDetails.getStatus());
                    applyInterviewStatusRule(existingCard);
                    return cardRepository.save(existingCard);
                })
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + id));
    }

    public void deleteCard(Long id, OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        if (isGoogleConnected(authentication)) {
            Card cardToDelete = new Card();
            cardToDelete.setId(id);
            googleSheetsService.saveCard(getAuthorizedClient(authentication), cardToDelete, true);
            return;
        }
        if (!cardRepository.existsById(id)) {
            throw new EntityNotFoundException("Card not found with id: " + id);
        }
        cardRepository.deleteById(id);
    }

    private void applyInterviewStatusRule(Card card) {
        String interviewDate = card.getInterviewDate();
        if (interviewDate != null && !interviewDate.isEmpty() && !"TBD".equalsIgnoreCase(interviewDate)) {
            card.setStatus(CardStatus.INTERVIEW_DATE_CONFIRMED);
        }
    }
}