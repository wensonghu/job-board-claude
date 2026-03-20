package com.example.jobboard.repository;

import com.example.jobboard.model.ChatSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatScheduleRepository extends JpaRepository<ChatSchedule, Short> {
    List<ChatSchedule> findAllByOrderByDayOfWeekAsc();
}
