package com.example.jobboard.repository;

import com.example.jobboard.model.SupportChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, Long> {
    List<SupportChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
