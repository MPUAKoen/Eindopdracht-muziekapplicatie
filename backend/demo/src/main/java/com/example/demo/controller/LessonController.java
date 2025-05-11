// src/main/java/com/example/demo/controller/LessonController.java
package com.example.demo.controller;

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
public class LessonController {

    private final LessonRepository lessonRepo;
    private final UserRepository userRepo;
    private final FileStorageService storage;

    public LessonController(LessonRepository lessonRepo,
            UserRepository userRepo,
            FileStorageService storage) {
        this.lessonRepo = lessonRepo;
        this.userRepo = userRepo;
        this.storage = storage;
    }

    /** Teachers book lessons for students by student‐ID */
    @PostMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public Lesson addLesson(
            @RequestParam String instrument,
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lessonDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) String homework,
            @RequestParam(required = false) List<MultipartFile> pdfFiles,
            Authentication authentication) {
        // Lookup teacher by email (principal)
        User teacher = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Teacher not found"));

        // Lookup student by ID
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        // Store uploaded PDFs (if any)
        List<String> filenames = (pdfFiles != null)
                ? storage.storeFiles(pdfFiles)
                : List.of();

        // Build and save lesson
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

    /** Teachers view all lessons they’ve scheduled */
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public List<Lesson> getByTeacher(Authentication authentication) {
        User teacher = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Teacher not found"));
        return lessonRepo.findByTeacher(teacher);
    }

    /** Students view their own lessons */
    @GetMapping("/student")
    @PreAuthorize("isAuthenticated()")
    public List<Lesson> getByStudent(Authentication authentication) {
        User student = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));
        return lessonRepo.findByStudent(student);
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
}
