package com.example.jobboard.repository;

import com.example.jobboard.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByGoogleSub(String googleSub);
    Optional<AppUser> findByUserKey(String userKey);
    Optional<AppUser> findByVerificationToken(String token);
    boolean existsByEmail(String email);
}
