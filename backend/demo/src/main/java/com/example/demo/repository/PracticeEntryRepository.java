package com.example.demo.repository;

import com.example.demo.model.PracticeEntry;
import com.example.demo.model.PracticeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeEntryRepository extends JpaRepository<PracticeEntry, Long> {

    List<PracticeEntry> findByPracticeLog(PracticeLog practiceLog);
}
