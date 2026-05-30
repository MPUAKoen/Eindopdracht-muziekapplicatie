package com.example.demo.controller;

import com.example.demo.Service.LessonService;
import com.example.demo.dto.LessonCreateRequest;
import com.example.demo.dto.LessonDetailDto;
import com.example.demo.dto.LessonSimpleView;
import com.example.demo.dto.LessonUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@CrossOrigin(origins = "http://localhost:5173")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonDetailDto> createLesson(
            @Valid @ModelAttribute LessonCreateRequest request,
            Authentication authentication
    ) {
        LessonDetailDto saved = lessonService.createLesson(request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonSimpleView>> getLessons(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonService.getLessons(scope, teacherId, studentId, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonDetailDto> getLesson(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(lessonService.getLesson(id, authentication));
    }

    @GetMapping("/{lessonId}/files/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getLessonFile(
            @PathVariable Long lessonId,
            @PathVariable String filename,
            Authentication authentication
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(lessonService.getLessonFile(lessonId, filename, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonDetailDto> replaceLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonService.updateLesson(id, request, authentication, false));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonDetailDto> patchLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonService.updateLesson(id, request, authentication, true));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication authentication) {
        lessonService.deleteLesson(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
