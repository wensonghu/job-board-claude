package com.example.jobboard.controller;

import com.example.jobboard.model.Card;
import com.example.jobboard.service.CardService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping
    public List<Card> getAllCards(@AuthenticationPrincipal OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        return cardService.getAllCards(authentication);
    }

    @PostMapping
    public Card createCard(@RequestBody Card card, @AuthenticationPrincipal OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        return cardService.createCard(card, authentication);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card cardDetails, @AuthenticationPrincipal OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        Card updatedCard = cardService.updateCard(id, cardDetails, authentication);
        return ResponseEntity.ok(updatedCard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id, @AuthenticationPrincipal OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        cardService.deleteCard(id, authentication);
        return ResponseEntity.noContent().build();
    }
}