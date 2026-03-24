package com.example.jobboard.repository;

import com.example.jobboard.model.UserSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSurveyRepository extends JpaRepository<UserSurvey, Long> {
    List<UserSurvey> findAllByOrderByCreatedAtDesc();
}
