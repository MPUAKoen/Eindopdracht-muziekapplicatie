package com.example.demo.controller;

import com.example.demo.Service.FileStorageService;
import com.example.demo.dto.LessonDetailDto;
import com.example.demo.dto.LessonSimpleView;
import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/lessons")
@CrossOrigin(origins = "http://localhost:5173")
public class LessonController {

    private static final DateTimeFormatter DUTCH_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    public LessonController(
            LessonRepository lessonRepository,
            UserRepository userRepository,
            FileStorageService storage
    ) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonDetailDto> createLesson(
            @RequestParam String instrument,
            @RequestParam Long studentId,
            @RequestParam String lessonDate,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String homework,
            @RequestParam(required = false) List<MultipartFile> pdfFiles,
            Authentication authentication
    ) {
        User teacher = requireCurrentUser(authentication);
        requireTeacher(teacher);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        validateTeacherOwnsStudent(teacher, student);

        Lesson lesson = new Lesson();
        lesson.setInstrument(requireText(instrument, "instrument"));
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setLessonDate(parseDate(lessonDate));
        lesson.setStartTime(parseTime(startTime));
        lesson.setEndTime(parseTime(endTime));
        lesson.setHomework(trimToNull(homework));
        lesson.setPdfFileNames(pdfFiles != null && !pdfFiles.isEmpty() ? storage.storeFiles(pdfFiles) : List.of());

        Lesson saved = lessonRepository.save(lesson);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(toDetailDto(saved));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonSimpleView>> getLessons(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication
    ) {
        User currentUser = requireCurrentUser(authentication);
        String resolvedScope = resolveScope(scope, currentUser);

        if ("teaching".equals(resolvedScope)) {
            Long targetTeacherId = resolveTeacherId(currentUser, teacherId);
            return ResponseEntity.ok(lessonRepository.findSimpleByTeacherId(targetTeacherId));
        }

        Long targetStudentId = resolveStudentId(currentUser, studentId);
        return ResponseEntity.ok(lessonRepository.findSimpleByStudentId(targetStudentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonDetailDto> getLesson(@PathVariable Long id, Authentication authentication) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        validateLessonAccess(lesson, requireCurrentUser(authentication), authentication);
        return ResponseEntity.ok(toDetailDto(lesson));
    }

    @GetMapping("/{lessonId}/files/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getLessonFile(
            @PathVariable Long lessonId,
            @PathVariable String filename,
            Authentication authentication
    ) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        validateLessonAccess(lesson, requireCurrentUser(authentication), authentication);

        if (lesson.getPdfFileNames() == null || !lesson.getPdfFileNames().contains(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        Path file = storage.load(filename);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonDetailDto> replaceLesson(
            @PathVariable Long id,
            @RequestBody LessonUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(updateLesson(id, request, authentication, false));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonDetailDto> patchLesson(
            @PathVariable Long id,
            @RequestBody LessonUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(updateLesson(id, request, authentication, true));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication authentication) {
        User currentUser = requireCurrentUser(authentication);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        validateLessonManagement(lesson, currentUser, authentication);
        lessonRepository.delete(lesson);
        return ResponseEntity.noContent().build();
    }

    private LessonDetailDto updateLesson(
            Long id,
            LessonUpdateRequest request,
            Authentication authentication,
            boolean partial
    ) {
        User currentUser = requireCurrentUser(authentication);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        validateLessonManagement(lesson, currentUser, authentication);
        applyUpdates(lesson, request, currentUser, partial, isAdmin(authentication));
        return toDetailDto(lessonRepository.save(lesson));
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

    private LessonDetailDto toDetailDto(Lesson lesson) {
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

    private User requireCurrentUser(Authentication authentication) {
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public static class LessonUpdateRequest {
        private String instrument;
        private Long studentId;
        private String lessonDate;
        private String startTime;
        private String endTime;
        private String homework;

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getLessonDate() {
            return lessonDate;
        }

        public void setLessonDate(String lessonDate) {
            this.lessonDate = lessonDate;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getHomework() {
            return homework;
        }

        public void setHomework(String homework) {
            this.homework = homework;
        }
    }
}
