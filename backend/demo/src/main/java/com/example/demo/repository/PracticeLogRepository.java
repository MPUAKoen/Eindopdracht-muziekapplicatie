package com.example.demo.repository;

import com.example.demo.model.PracticeLog;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeLogRepository extends JpaRepository<PracticeLog, Long> {

    Optional<PracticeLog> findByUser(User user);
}
