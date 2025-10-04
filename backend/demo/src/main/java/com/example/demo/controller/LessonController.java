// src/main/java/com/example/demo/controller/LessonController.java
package com.example.demo.controller;

import com.example.demo.dto.LessonSimpleView;
import com.example.demo.dto.LessonDetailDto;
import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.Service.FileStorageService;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.List;

@RestController
@RequestMapping("/api/lesson")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class LessonController {

    private final LessonRepository lessonRepo;
    private final UserRepository userRepo;
    private final FileStorageService storage;

    public LessonController(LessonRepository lessonRepo,
                            UserRepository userRepo,
                            FileStorageService storage) {
        this.lessonRepo = lessonRepo;
        this.userRepo   = userRepo;
        this.storage    = storage;
    }

    /** Teachers book lessons for students */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('TEACHER')")
    public Lesson addLesson(
            @RequestParam String instrument,
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lessonDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) String homework,
            @RequestParam(required = false) List<MultipartFile> pdfFiles,
            Authentication authentication) {

        User teacher = userRepo.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        User student = userRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<String> filenames = (pdfFiles != null && !pdfFiles.isEmpty())
            ? storage.storeFiles(pdfFiles)
            : List.of();

        Lesson lesson = new Lesson();
        lesson.setInstrument(instrument);
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setLessonDate(lessonDate);
        lesson.setStartTime(startTime);
        lesson.setEndTime(endTime);
        lesson.setHomework(homework);
        lesson.setPdfFileNames(filenames);

        return lessonRepo.save(lesson);
    }

    /** Teachers view all lessons */
    @GetMapping("/teacher")
    @PreAuthorize("hasAuthority('TEACHER')")
    public List<LessonSimpleView> getByTeacher(Authentication auth) {
        User teacher = userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        return lessonRepo.findSimpleByTeacherId(teacher.getId());
    }

    /** Students view their own lessons */
    @GetMapping("/student")
    @PreAuthorize("hasAnyAuthority('USER','STUDENT','TEACHER')")
    public List<LessonSimpleView> getByStudent(Authentication auth) {
        User student = userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return lessonRepo.findSimpleByStudentId(student.getId());
    }

    /** Get single lesson by ID (for HomeworkPage) */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonDetailDto> getLesson(@PathVariable Long id, Authentication auth) {
        Lesson lesson = lessonRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        User me = userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !lesson.getStudent().getId().equals(me.getId()) &&
            !lesson.getTeacher().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        // Convert entity to DTO
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

    /** Download a lesson’s PDF */
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

    /** Fetch all lessons for current user */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public List<Lesson> getAllForCurrent(Authentication auth) {
        User me = userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return lessonRepo.findAll().stream()
            .filter(l ->
                l.getStudent().getId().equals(me.getId()) ||
                l.getTeacher().getId().equals(me.getId())
            )
            .toList();
    }

    /** Delete a lesson */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEACHER','ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication auth) {
        User me = userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Lesson lesson = lessonRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !lesson.getTeacher().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete lessons you teach.");
        }

        lessonRepo.delete(lesson);
        return ResponseEntity.noContent().build();
    }
}
