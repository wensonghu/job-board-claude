package com.example.jobboard.repository;

import com.example.jobboard.model.SupportChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportChatSessionRepository extends JpaRepository<SupportChatSession, Long> {
    Optional<SupportChatSession> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<SupportChatSession> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
