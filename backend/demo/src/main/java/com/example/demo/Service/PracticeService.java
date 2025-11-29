package com.example.demo.Service;

import com.example.demo.dto.PracticeEntryRequest;
import com.example.demo.dto.PracticeSummaryDto;
import com.example.demo.model.PracticeEntry;
import com.example.demo.model.PracticeLog;
import com.example.demo.model.PracticeType;
import com.example.demo.model.User;
import com.example.demo.repository.PracticeEntryRepository;
// import com.example.demo.repository.PracticeLogRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PracticeService {

    // note the fully qualified type here
    private final com.example.demo.repository.PracticeLogRepository logRepository;
    private final PracticeEntryRepository entryRepository;
    private final UserRepository userRepository;

    public PracticeService(
            com.example.demo.repository.PracticeLogRepository logRepository,
            PracticeEntryRepository entryRepository,
            UserRepository userRepository
    ) {
        this.logRepository = logRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    public void addEntryForCurrentUser(PracticeEntryRequest request) {
        User user = getCurrentUser();

        PracticeLog log = logRepository
                .findByUser(user)
                .orElseGet(() -> {
                    PracticeLog newLog = new PracticeLog();
                    newLog.setUser(user);
                    return logRepository.save(newLog);
                });

        PracticeEntry entry = new PracticeEntry();
        entry.setPracticeLog(log);
        entry.setType(
                request.getType() != null ? request.getType() : PracticeType.PRACTICE
        );
        entry.setMinutes(request.getMinutes());
        entry.setDateTime(
                request.getDateTime() != null ? request.getDateTime() : LocalDateTime.now()
        );

        entryRepository.save(entry);
    }

    public PracticeSummaryDto getSummaryForCurrentUser() {
        User user = getCurrentUser();

        PracticeLog log = logRepository
                .findByUser(user)
                .orElseGet(() -> {
                    PracticeLog newLog = new PracticeLog();
                    newLog.setUser(user);
                    return logRepository.save(newLog);
                });

        List<PracticeEntry> entries = entryRepository.findByPracticeLog(log);

        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime start7 = today.minusDays(6).atStartOfDay();
        LocalDateTime start30 = today.minusDays(29).atStartOfDay();
        LocalDateTime start365 = today.minusDays(364).atStartOfDay();

        PracticeSummaryDto.RangeSummary practice = new PracticeSummaryDto.RangeSummary();
        PracticeSummaryDto.RangeSummary guided = new PracticeSummaryDto.RangeSummary();
        PracticeSummaryDto.RangeSummary listened = new PracticeSummaryDto.RangeSummary();

        for (PracticeEntry entry : entries) {
            PracticeSummaryDto.RangeSummary target;
            if (entry.getType() == PracticeType.PRACTICE) {
                target = practice;
            } else if (entry.getType() == PracticeType.GUIDED) {
                target = guided;
            } else {
                target = listened;
            }

            long minutes = entry.getMinutes();
            LocalDateTime t = entry.getDateTime();

            target.setAllTime(target.getAllTime() + minutes);

            if (!t.isBefore(start365)) {
                target.setLast365Days(target.getLast365Days() + minutes);
            }
            if (!t.isBefore(start30)) {
                target.setLast30Days(target.getLast30Days() + minutes);
            }
            if (!t.isBefore(start7)) {
                target.setLast7Days(target.getLast7Days() + minutes);
            }
            if (!t.isBefore(startToday)) {
                target.setToday(target.getToday() + minutes);
            }
        }

        PracticeSummaryDto dto = new PracticeSummaryDto();
        dto.setPracticeHours(practice);
        dto.setGuidedLessonHours(guided);
        dto.setListenedHours(listened);
        return dto;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }
}
