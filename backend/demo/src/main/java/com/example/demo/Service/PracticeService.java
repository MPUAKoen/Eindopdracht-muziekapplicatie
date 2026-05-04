package com.example.demo.Service;

import com.example.demo.dto.PracticeEntryRequest;
import com.example.demo.dto.PracticeSummaryDto;
import com.example.demo.model.PracticeEntry;
import com.example.demo.model.PracticeLog;
import com.example.demo.model.PracticeType;
import com.example.demo.model.User;
import com.example.demo.repository.PracticeEntryRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PracticeService {

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

    public PracticeEntry addEntryForCurrentUser(PracticeEntryRequest request) {
        PracticeLog log = getOrCreateLogForCurrentUser();
        PracticeEntry entry = new PracticeEntry();
        entry.setPracticeLog(log);
        applyRequest(entry, request, false);

        return entryRepository.save(entry);
    }

    public List<PracticeEntry> getEntriesForCurrentUser() {
        PracticeLog log = getOrCreateLogForCurrentUser();
        return entryRepository.findByPracticeLogOrderByDateTimeDesc(log);
    }

    public PracticeEntry getEntryForCurrentUser(Long id) {
        PracticeLog log = getOrCreateLogForCurrentUser();
        return entryRepository.findByIdAndPracticeLog(id, log)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Practice entry not found"));
    }

    public PracticeEntry replaceEntryForCurrentUser(Long id, PracticeEntryRequest request) {
        PracticeEntry entry = getEntryForCurrentUser(id);
        applyRequest(entry, request, false);
        return entryRepository.save(entry);
    }

    public PracticeEntry updateEntryForCurrentUser(Long id, PracticeEntryRequest request) {
        PracticeEntry entry = getEntryForCurrentUser(id);
        applyRequest(entry, request, true);
        return entryRepository.save(entry);
    }

    public void deleteEntryForCurrentUser(Long id) {
        PracticeEntry entry = getEntryForCurrentUser(id);
        entryRepository.delete(entry);
    }

    public PracticeSummaryDto getSummaryForCurrentUser() {
        PracticeLog log = getOrCreateLogForCurrentUser();
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

    private void applyRequest(PracticeEntry entry, PracticeEntryRequest request, boolean partial) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Practice entry payload is required");
        }

        if (partial) {
            if (request.getType() != null) {
                entry.setType(request.getType());
            }
            if (request.getMinutes() != null) {
                entry.setMinutes(validateMinutes(request.getMinutes()));
            }
            if (request.getDateTime() != null) {
                entry.setDateTime(request.getDateTime());
            }
            return;
        }

        entry.setType(request.getType() != null ? request.getType() : PracticeType.PRACTICE);
        entry.setMinutes(validateMinutes(request.getMinutes()));
        entry.setDateTime(request.getDateTime() != null ? request.getDateTime() : LocalDateTime.now());
    }

    private int validateMinutes(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Minutes must be greater than 0");
        }
        return minutes;
    }

    private PracticeLog getOrCreateLogForCurrentUser() {
        User user = getCurrentUser();
        return logRepository.findByUser(user)
                .orElseGet(() -> {
                    PracticeLog newLog = new PracticeLog();
                    newLog.setUser(user);
                    return logRepository.save(newLog);
                });
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
