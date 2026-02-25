package com.example.jobboard.repository;

import com.example.jobboard.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserId(Long userId);
    Optional<Card> findByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}