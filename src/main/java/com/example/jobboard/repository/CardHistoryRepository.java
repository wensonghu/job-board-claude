package com.example.jobboard.repository;

import com.example.jobboard.model.CardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardHistoryRepository extends JpaRepository<CardHistory, Long> {
    List<CardHistory> findByCardIdOrderByChangedAtAsc(Long cardId);
    List<CardHistory> findByUserIdOrderByChangedAtDesc(Long userId);
}
