package com.example.demo.Service;

import com.example.demo.dto.LessonCreateRequest;
import com.example.demo.dto.LessonDetailDto;
import com.example.demo.dto.LessonSimpleView;
import com.example.demo.dto.LessonUpdateRequest;
import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class LessonService {

    private static final DateTimeFormatter DUTCH_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    @Autowired
    public LessonService(
            LessonRepository lessonRepository,
            UserRepository userRepository,
            FileStorageService storage
    ) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    public LessonService(LessonRepository lessonRepository, UserRepository userRepository) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.storage = null;
    }

    public Lesson saveLesson(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public Lesson getLessonById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    public List<?> getLessonsByTeacherId(Long teacherId) {
        return lessonRepository.findSimpleByTeacherId(teacherId);
    }

    public LessonDetailDto createLesson(LessonCreateRequest request, Authentication authentication) {
        User teacher = requireCurrentUser(authentication);
        requireTeacher(teacher);

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        validateTeacherOwnsStudent(teacher, student);

        Lesson lesson = new Lesson();
        lesson.setInstrument(requireText(request.getInstrument(), "instrument"));
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setLessonDate(parseDate(request.getLessonDate()));
        lesson.setStartTime(parseTime(request.getStartTime()));
        lesson.setEndTime(parseTime(request.getEndTime()));
        lesson.setHomework(trimToNull(request.getHomework()));
        lesson.setPdfFileNames(
                request.getPdfFiles() != null && !request.getPdfFiles().isEmpty()
                        ? requireStorage().storeFiles(request.getPdfFiles())
                        : List.of()
        );

        validateLessonTimeSlot(lesson, null);
        return toDetailDto(lessonRepository.save(lesson));
    }

    public List<LessonSimpleView> getLessons(
            String scope,
            Long teacherId,
            Long studentId,
            Authentication authentication
    ) {
        User currentUser = requireCurrentUser(authentication);
        String resolvedScope = resolveScope(scope, currentUser);

        if ("teaching".equals(resolvedScope)) {
            Long targetTeacherId = resolveTeacherId(currentUser, teacherId);
            return lessonRepository.findSimpleByTeacherId(targetTeacherId);
        }

        Long targetStudentId = resolveStudentId(currentUser, studentId);
        return lessonRepository.findSimpleByStudentId(targetStudentId);
    }

    public LessonDetailDto getLesson(Long id, Authentication authentication) {
        Lesson lesson = getLessonById(id);
        validateLessonAccess(lesson, requireCurrentUser(authentication), authentication);
        return toDetailDto(lesson);
    }

    public Resource getLessonFile(Long lessonId, String filename, Authentication authentication) {
        Lesson lesson = getLessonById(lessonId);
        validateLessonAccess(lesson, requireCurrentUser(authentication), authentication);

        if (lesson.getPdfFileNames() == null || !lesson.getPdfFileNames().contains(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        Path file = requireStorage().load(filename);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return resource;
    }

    public LessonDetailDto updateLesson(
            Long id,
            LessonUpdateRequest request,
            Authentication authentication,
            boolean partial
    ) {
        User currentUser = requireCurrentUser(authentication);
        Lesson lesson = getLessonById(id);

        validateLessonManagement(lesson, currentUser, authentication);
        applyUpdates(lesson, request, currentUser, partial, isAdmin(authentication));
        validateLessonTimeSlot(lesson, lesson.getId());
        return toDetailDto(lessonRepository.save(lesson));
    }

    public void deleteLesson(Long id, Authentication authentication) {
        User currentUser = requireCurrentUser(authentication);
        Lesson lesson = getLessonById(id);

        validateLessonManagement(lesson, currentUser, authentication);
        lessonRepository.delete(lesson);
    }

    public void deleteLesson(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        lessonRepository.delete(lesson);
    }

    public Lesson createLesson(String instrument, Long teacherId, Long studentId,
                               LocalDate lessonDate, LocalTime startTime, LocalTime endTime, String homework) {

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Lesson lesson = new Lesson();
        lesson.setInstrument(instrument);
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setLessonDate(lessonDate);
        lesson.setStartTime(startTime);
        lesson.setEndTime(endTime);
        lesson.setHomework(homework);

        validateLessonTimeSlot(lesson, null);
        return lessonRepository.save(lesson);
    }

    public LessonDetailDto toDetailDto(Lesson lesson) {
        return new LessonDetailDto(
                lesson.getId(),
                lesson.getInstrument(),
                lesson.getTeacher().getId(),
                lesson.getTeacher().getName(),
                lesson.getStudent().getId(),
                lesson.getStudent().getName(),
                lesson.getLessonDate(),
                lesson.getStartTime(),
                lesson.getEndTime(),
                lesson.getHomework(),
                lesson.getPdfFileNames()
        );
    }

    private void applyUpdates(
            Lesson lesson,
            LessonUpdateRequest request,
            User currentUser,
            boolean partial,
            boolean admin
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson payload is required");
        }

        if (!partial || request.getInstrument() != null) {
            lesson.setInstrument(requireText(request.getInstrument(), "instrument"));
        }
        if (!partial || request.getLessonDate() != null) {
            lesson.setLessonDate(parseDate(request.getLessonDate()));
        }
        if (!partial || request.getStartTime() != null) {
            lesson.setStartTime(parseTime(request.getStartTime()));
        }
        if (!partial || request.getEndTime() != null) {
            lesson.setEndTime(parseTime(request.getEndTime()));
        }
        if (!partial || request.getHomework() != null) {
            lesson.setHomework(trimToNull(request.getHomework()));
        }
        if (!partial || request.getStudentId() != null) {
            if (request.getStudentId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
            }

            User student = userRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

            if (!admin) {
                validateTeacherOwnsStudent(currentUser, student);
            }
            lesson.setStudent(student);
        }
    }

    private String resolveScope(String scope, User currentUser) {
        if (scope == null || scope.isBlank()) {
            return hasRole(currentUser, "TEACHER") ? "teaching" : "learning";
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        if (!List.of("teaching", "learning").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be teaching or learning");
        }
        return normalized;
    }

    private Long resolveTeacherId(User currentUser, Long teacherId) {
        if (teacherId != null) {
            if (!hasRole(currentUser, "ADMIN") && !teacherId.equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own teaching lessons");
            }
            return teacherId;
        }
        if (!hasRole(currentUser, "TEACHER") && !hasRole(currentUser, "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can view teaching lessons");
        }
        return currentUser.getId();
    }

    private Long resolveStudentId(User currentUser, Long studentId) {
        if (studentId != null) {
            if (hasRole(currentUser, "ADMIN")) {
                return studentId;
            }
            if (!studentId.equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own learning lessons");
            }
            return studentId;
        }
        return currentUser.getId();
    }

    private void validateLessonAccess(Lesson lesson, User currentUser, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        if (lesson.getStudent().getId().equals(currentUser.getId())) {
            return;
        }
        if (lesson.getTeacher().getId().equals(currentUser.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
    }

    private void validateLessonManagement(Lesson lesson, User currentUser, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        if (!lesson.getTeacher().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own lessons");
        }
    }

    private void validateTeacherOwnsStudent(User teacher, User student) {
        if (!hasRole(student, "USER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lessons can only be scheduled for students");
        }
        if (student.getTeacher() == null || !teacher.getId().equals(student.getTeacher().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage lessons for your own students");
        }
    }

    private User requireCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireTeacher(User user) {
        if (!hasRole(user, "TEACHER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can manage lessons");
        }
    }

    private boolean hasRole(User user, String role) {
        return role.equalsIgnoreCase(user.getRole());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lessonDate is required");
        }
        try {
            return LocalDate.parse(value, DUTCH_DATE);
        } catch (Exception ignored) {
            try {
                return LocalDate.parse(value);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lessonDate must be dd-MM-yyyy or yyyy-MM-dd");
            }
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time fields are required");
        }
        try {
            return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time must be HH:mm or HH:mm:ss");
        }
    }

    private void validateLessonTimeSlot(Lesson lesson, Long excludeLessonId) {
        if (lesson.getStartTime() == null || lesson.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end time are required");
        }
        if (!lesson.getStartTime().isBefore(lesson.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        boolean overlapsTooMuch = lessonRepository.findOverlappingLessons(
                lesson.getTeacher().getId(),
                lesson.getStudent().getId(),
                lesson.getLessonDate(),
                lesson.getStartTime(),
                lesson.getEndTime(),
                excludeLessonId
        ).stream().anyMatch(existingLesson -> overlapMinutes(lesson, existingLesson) > 10);

        if (overlapsTooMuch) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This teacher or student already has a lesson at that time"
            );
        }
    }

    private long overlapMinutes(Lesson lesson, Lesson existingLesson) {
        LocalTime overlapStart = lesson.getStartTime().isAfter(existingLesson.getStartTime())
                ? lesson.getStartTime()
                : existingLesson.getStartTime();
        LocalTime overlapEnd = lesson.getEndTime().isBefore(existingLesson.getEndTime())
                ? lesson.getEndTime()
                : existingLesson.getEndTime();

        return Math.max(0, ChronoUnit.MINUTES.between(overlapStart, overlapEnd));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private FileStorageService requireStorage() {
        if (storage == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File storage is not configured");
        }
        return storage;
    }
}
