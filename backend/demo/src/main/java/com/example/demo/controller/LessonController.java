package com.example.demo.controller;

import com.example.demo.Service.FileStorageService;
import com.example.demo.model.Lesson;
import com.example.demo.repository.LessonRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/lesson")
public class LessonController {

    private final LessonRepository lessonRepo;
    private final FileStorageService storage;

    public LessonController(LessonRepository lessonRepo, FileStorageService storage) {
        this.lessonRepo = lessonRepo;
        this.storage = storage;
    }

    /**
     * Teachers book lessons for students
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public Lesson addLesson(
            @RequestParam String instrument,
            @RequestParam String studentName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lessonDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) String homework,
            @RequestParam(required = false) List<MultipartFile> pdfFiles,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String teacherName = principal.getUsername();

        List<String> filenames = (pdfFiles != null)
                ? storage.storeFiles(pdfFiles)
                : List.of();

        Lesson lesson = new Lesson();
        lesson.setInstrument(instrument);
        lesson.setStudentName(studentName);
        lesson.setTeacherName(teacherName);
        lesson.setLessonDate(lessonDate);
        lesson.setStartTime(startTime);
        lesson.setEndTime(endTime);
        lesson.setHomework(homework);
        lesson.setPdfFileNames(filenames);

        return lessonRepo.save(lesson);
    }

    /**
     * Teachers view all lessons they've booked
     */
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public List<Lesson> getByTeacher(@AuthenticationPrincipal UserDetails principal) {
        return lessonRepo.findByTeacherName(principal.getUsername());
    }

    /**
     * Students view their own lessons
     */
    @GetMapping("/student")
    @PreAuthorize("isAuthenticated()")
    public List<Lesson> getByStudent(@AuthenticationPrincipal UserDetails principal) {
        return lessonRepo.findByStudentName(principal.getUsername());
    }

    /**
     * Download a lesson’s PDF
     */
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

