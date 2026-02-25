package com.example.jobboard.repository;

import com.example.jobboard.model.BroadcastMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, Long> {

    Optional<BroadcastMessage> findTopByActiveTrueAndCreatedAtAfterOrderByCreatedAtDesc(
            OffsetDateTime cutoff);

    @Modifying
    @Transactional
    @Query("UPDATE BroadcastMessage b SET b.active = false")
    void deactivateAll();
}
