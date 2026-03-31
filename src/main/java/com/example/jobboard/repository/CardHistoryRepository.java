package com.example.jobboard.repository;

import com.example.jobboard.model.CardHistory;
import com.example.jobboard.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardHistoryRepository extends JpaRepository<CardHistory, Long> {
    List<CardHistory> findByCardIdOrderByChangedAtAsc(Long cardId);
    List<CardHistory> findByUserIdOrderByChangedAtDesc(Long userId);
    List<CardHistory> findByUserIdAndStatusAndInterviewDateIsNotNull(Long userId, CardStatus status);
    List<CardHistory> findByUserIdAndStatusOrderByChangedAtAsc(Long userId, CardStatus status);
}
