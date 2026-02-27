package com.example.jobboard.repository;

import com.example.jobboard.model.AlertDismissal;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface AlertDismissalRepository extends JpaRepository<AlertDismissal, Long> {

    boolean existsByUserIdAndAlertKey(Long userId, String alertKey);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AlertDismissal d WHERE d.userId = :userId AND d.alertKey NOT IN :activeKeys")
    void deleteByUserIdAndAlertKeyNotIn(@Param("userId") Long userId,
                                        @Param("activeKeys") Collection<String> activeKeys);
}
