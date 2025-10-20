package com.example.demo.controller;

import com.example.demo.dto.LessonSimpleView;
import com.example.demo.dto.LessonDetailDto;
import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.Service.FileStorageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lesson")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class LessonController {

    private final LessonRepository lessonRepo;
    private final UserRepository userRepo;
    private final FileStorageService storage;

    private static final DateTimeFormatter DUTCH_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LessonController(LessonRepository lessonRepo,
                            UserRepository userRepo,
                            FileStorageService storage) {
        this.lessonRepo = lessonRepo;
        this.userRepo = userRepo;
        this.storage = storage;
    }

    /** Teachers can add lessons for students */
    @PostMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public Lesson addLesson(
            @RequestParam String instrument,
            @RequestParam Long studentId,
            // accept strings so we can parse both dd-MM-yyyy and ISO
            @RequestParam String lessonDate,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String homework,
            @RequestParam(required = false) List<MultipartFile> pdfFiles,
            Authentication authentication) {

        User teacher = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can add lessons");
        }

        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // Parse date: Dutch first, ISO fallback
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(lessonDate, DUTCH_DATE);
        } catch (Exception e) {
            parsedDate = LocalDate.parse(lessonDate);
        }

        // Parse time: add seconds if missing
        LocalTime start = LocalTime.parse(startTime.length() == 5 ? startTime + ":00" : startTime);
        LocalTime end   = LocalTime.parse(endTime.length() == 5 ? endTime + ":00" : endTime);

        List<String> filenames = (pdfFiles != null && !pdfFiles.isEmpty())
                ? storage.storeFiles(pdfFiles)
                : List.of();

        Lesson lesson = new Lesson();
        lesson.setInstrument(instrument);
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setLessonDate(parsedDate);
        lesson.setStartTime(start);
        lesson.setEndTime(end);
        lesson.setHomework(homework);
        lesson.setPdfFileNames(filenames);

        return lessonRepo.save(lesson);
    }

    /** Teachers can view their lessons */
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public List<LessonSimpleView> getByTeacher(Authentication auth) {
        User teacher = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        return lessonRepo.findSimpleByTeacherId(teacher.getId());
    }

    /** Students can view their lessons */
    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('USER','STUDENT','TEACHER')")
    public List<LessonSimpleView> getByStudent(Authentication auth) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return lessonRepo.findSimpleByStudentId(student.getId());
    }

    /** Get lesson details */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonDetailDto> getLesson(@PathVariable Long id, Authentication auth) {
        Lesson lesson = lessonRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        User me = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !lesson.getStudent().getId().equals(me.getId())
                && !lesson.getTeacher().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        LessonDetailDto dto = new LessonDetailDto(
                lesson.getId(),
                lesson.getInstrument(),
                lesson.getTeacher().getName(),
                lesson.getStudent().getName(),
                lesson.getLessonDate(),
                lesson.getStartTime(),
                lesson.getEndTime(),
                lesson.getHomework(),
                lesson.getPdfFileNames()
        );

        return ResponseEntity.ok(dto);
    }

    /** Download lesson PDF */
    @GetMapping("/file/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Path file = storage.load(filename);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /** Delete lesson (teacher/admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication auth) {
        User me = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Lesson lesson = lessonRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !lesson.getTeacher().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own lessons");
        }

        lessonRepo.delete(lesson);
        return ResponseEntity.noContent().build();
    }

    /** Update lesson fields inline (teacher or admin only) */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Map<String, Object>> updateLesson(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication auth) {

        User me = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Lesson lesson = lessonRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !lesson.getTeacher().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own lessons");
        }

        // Apply updates dynamically
        if (updates.containsKey("instrument")) {
            lesson.setInstrument((String) updates.get("instrument"));
        }

        if (updates.containsKey("lessonDate")) {
            String dateStr = String.valueOf(updates.get("lessonDate"));
            try {
                lesson.setLessonDate(LocalDate.parse(dateStr, DUTCH_DATE)); // dd-MM-yyyy
            } catch (Exception e) {
                lesson.setLessonDate(LocalDate.parse(dateStr));             // ISO fallback
            }
        }

        if (updates.containsKey("startTime")) {
            String start = String.valueOf(updates.get("startTime"));
            lesson.setStartTime(LocalTime.parse(start.length() == 5 ? start + ":00" : start));
        }

        if (updates.containsKey("endTime")) {
            String end = String.valueOf(updates.get("endTime"));
            lesson.setEndTime(LocalTime.parse(end.length() == 5 ? end + ":00" : end));
        }

        if (updates.containsKey("homework")) {
            lesson.setHomework((String) updates.get("homework"));
        }

        if (updates.containsKey("studentId")) {
            Long newStudentId = Long.valueOf(String.valueOf(updates.get("studentId")));
            User newStudent = userRepo.findById(newStudentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            lesson.setStudent(newStudent);
        }

        Lesson saved = lessonRepo.save(lesson);

        // Return a lightweight map (avoid serializing lazy proxies)
        Map<String, Object> payload = Map.of(
                "id", saved.getId(),
                "instrument", saved.getInstrument(),
                "studentId", saved.getStudent().getId(),
                "teacherId", saved.getTeacher().getId(),
                "lessonDate", saved.getLessonDate().format(DUTCH_DATE),
                "startTime", saved.getStartTime().toString(),
                "endTime", saved.getEndTime().toString(),
                "homework", saved.getHomework()
        );

        return ResponseEntity.ok(payload);
    }
}
