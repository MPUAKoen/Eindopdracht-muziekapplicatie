package com.example.demo.Service;

import com.example.demo.dto.PracticeEntryRequest;
import com.example.demo.dto.PracticeSummaryDto;
import com.example.demo.model.PracticeEntry;
import com.example.demo.model.PracticeLog;
import com.example.demo.model.PracticeType;
import com.example.demo.model.User;
import com.example.demo.repository.PracticeEntryRepository;
import com.example.demo.repository.PracticeLogRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests voor PracticeService
 * AAA patroon: Arrange, Act, Assert
 */
@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    @Mock
    private PracticeLogRepository logRepository;

    @Mock
    private PracticeEntryRepository entryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PracticeService practiceService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Hulpmethode om een ingelogde gebruiker te simuleren.
     */
    private User mockAuthenticatedUser(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(email);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        User user = new User();
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void addEntryForCurrentUser_createsNewLogWhenNoneExists() {
        // Arrange
        String email = "user1@mail.com";
        User user = mockAuthenticatedUser(email);

        // logRepository.findByUser(...) geeft empty terug zodat er een nieuw log wordt aangemaakt
        when(logRepository.findByUser(user)).thenReturn(Optional.empty());
        when(logRepository.save(any(PracticeLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PracticeEntryRequest request = new PracticeEntryRequest();
        request.setType(PracticeType.GUIDED);
        request.setMinutes(30);
        LocalDateTime customTime = LocalDateTime.now().minusHours(1);
        request.setDateTime(customTime);

        // Act
        practiceService.addEntryForCurrentUser(request);

        // Assert
        ArgumentCaptor<PracticeEntry> entryCaptor = ArgumentCaptor.forClass(PracticeEntry.class);
        verify(entryRepository).save(entryCaptor.capture());

        PracticeEntry saved = entryCaptor.getValue();
        assertEquals(PracticeType.GUIDED, saved.getType());
        assertEquals(30, saved.getMinutes());
        assertEquals(customTime, saved.getDateTime());
        assertNotNull(saved.getPracticeLog());
        // controle dat log aan de juiste user hangt
        assertEquals(user, saved.getPracticeLog().getUser());
    }

    @Test
    void addEntryForCurrentUser_usesDefaultsWhenTypeAndDateMissing() {
        // Arrange
        String email = "user2@mail.com";
        User user = mockAuthenticatedUser(email);

        PracticeLog existingLog = new PracticeLog();
        existingLog.setUser(user);
        when(logRepository.findByUser(user)).thenReturn(Optional.of(existingLog));

        PracticeEntryRequest request = new PracticeEntryRequest();
        request.setMinutes(45);
        // type en dateTime blijven null

        // Act
        practiceService.addEntryForCurrentUser(request);

        // Assert
        ArgumentCaptor<PracticeEntry> entryCaptor = ArgumentCaptor.forClass(PracticeEntry.class);
        verify(entryRepository).save(entryCaptor.capture());

        PracticeEntry saved = entryCaptor.getValue();
        assertEquals(PracticeType.PRACTICE, saved.getType(), "Default type moet PRACTICE zijn");
        assertEquals(45, saved.getMinutes());
        assertNotNull(saved.getDateTime(), "Default dateTime moet gezet worden");
        assertEquals(existingLog, saved.getPracticeLog());
    }

    @Test
    void getSummaryForCurrentUser_createsLogWhenMissingAndReturnsZeroes() {
        // Arrange
        String email = "user3@mail.com";
        User user = mockAuthenticatedUser(email);

        when(logRepository.findByUser(user)).thenReturn(Optional.empty());
        when(logRepository.save(any(PracticeLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(entryRepository.findByPracticeLog(any(PracticeLog.class)))
                .thenReturn(List.of());

        // Act
        PracticeSummaryDto summary = practiceService.getSummaryForCurrentUser();

        // Assert
        assertNotNull(summary);
        assertEquals(0, summary.getPracticeHours().getAllTime());
        assertEquals(0, summary.getGuidedLessonHours().getAllTime());
        assertEquals(0, summary.getListenedHours().getAllTime());
    }

    @Test
    void getSummaryForCurrentUser_calculatesMinutesForAllRangesAndTypes() {
        // Arrange
        String email = "user4@mail.com";
        User user = mockAuthenticatedUser(email);

        PracticeLog log = new PracticeLog();
        log.setUser(user);
        when(logRepository.findByUser(user)).thenReturn(Optional.of(log));

        LocalDate today = LocalDate.now();

        PracticeEntry todayPractice = new PracticeEntry();
        todayPractice.setPracticeLog(log);
        todayPractice.setType(PracticeType.PRACTICE);
        todayPractice.setMinutes(30);
        todayPractice.setDateTime(today.atTime(10, 0));

        PracticeEntry last7Guided = new PracticeEntry();
        last7Guided.setPracticeLog(log);
        last7Guided.setType(PracticeType.GUIDED);
        last7Guided.setMinutes(20);
        last7Guided.setDateTime(today.minusDays(3).atTime(9, 0));

        PracticeEntry last30Listened = new PracticeEntry();
        last30Listened.setPracticeLog(log);
        last30Listened.setType(PracticeType.LISTENED);
        last30Listened.setMinutes(40);
        last30Listened.setDateTime(today.minusDays(10).atTime(18, 0));

        PracticeEntry last365Practice = new PracticeEntry();
        last365Practice.setPracticeLog(log);
        last365Practice.setType(PracticeType.PRACTICE);
        last365Practice.setMinutes(50);
        last365Practice.setDateTime(today.minusDays(100).atTime(12, 0));

        PracticeEntry olderGuided = new PracticeEntry();
        olderGuided.setPracticeLog(log);
        olderGuided.setType(PracticeType.GUIDED);
        olderGuided.setMinutes(60);
        olderGuided.setDateTime(today.minusDays(400).atTime(12, 0));

        when(entryRepository.findByPracticeLog(log))
                .thenReturn(List.of(
                        todayPractice,
                        last7Guided,
                        last30Listened,
                        last365Practice,
                        olderGuided
                ));

        // Act
        PracticeSummaryDto summary = practiceService.getSummaryForCurrentUser();

        // Assert
        PracticeSummaryDto.RangeSummary practice = summary.getPracticeHours();
        PracticeSummaryDto.RangeSummary guided = summary.getGuidedLessonHours();
        PracticeSummaryDto.RangeSummary listened = summary.getListenedHours();

        // PRACTICE totaal: 30 + 50
        assertEquals(80, practice.getAllTime());
        // vandaag: 30
        assertEquals(30, practice.getToday());
        // laatste 7: ook alleen 30
        assertEquals(30, practice.getLast7Days());
        // laatste 30: 30
        assertEquals(30, practice.getLast30Days());
        // laatste 365: 30 + 50
        assertEquals(80, practice.getLast365Days());

        // GUIDED totaal: 20 + 60
        assertEquals(80, guided.getAllTime());
        // laatste 7: 20
        assertEquals(20, guided.getLast7Days());
        // laatste 30: 20
        assertEquals(20, guided.getLast30Days());
        // laatste 365: alleen 20 (60 is ouder dan 365)
        assertEquals(20, guided.getLast365Days());
        // vandaag: 0
        assertEquals(0, guided.getToday());

        // LISTENED totaal: 40
        assertEquals(40, listened.getAllTime());
        assertEquals(0, listened.getToday());
        assertEquals(0, listened.getLast7Days());
        assertEquals(40, listened.getLast30Days());
        assertEquals(40, listened.getLast365Days());
    }

    @Test
    void addEntryForCurrentUser_withoutAuthentication_throwsIllegalStateException() {
        // Arrange
        SecurityContextHolder.clearContext();
        PracticeEntryRequest request = new PracticeEntryRequest();
        request.setMinutes(10);

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> practiceService.addEntryForCurrentUser(request));
    }

    @Test
    void addEntryForCurrentUser_userNotFound_throwsIllegalStateException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("missing@mail.com");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        PracticeEntryRequest request = new PracticeEntryRequest();
        request.setMinutes(15);

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> practiceService.addEntryForCurrentUser(request));
    }
}
