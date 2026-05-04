package com.example.demo.controller;

import com.example.demo.Service.PracticeService;
import com.example.demo.dto.PracticeEntryRequest;
import com.example.demo.dto.PracticeSummaryDto;
import com.example.demo.model.PracticeEntry;
import com.example.demo.model.PracticeType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/practice-entries")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PracticeEntryResponse>> getPracticeEntries() {
        List<PracticeEntryResponse> entries = practiceService.getEntriesForCurrentUser().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeEntryResponse> getPracticeEntry(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(practiceService.getEntryForCurrentUser(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeEntryResponse> addPracticeEntry(@RequestBody PracticeEntryRequest request) {
        PracticeEntry created = practiceService.addEntryForCurrentUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeEntryResponse> replacePracticeEntry(
            @PathVariable Long id,
            @RequestBody PracticeEntryRequest request
    ) {
        return ResponseEntity.ok(toResponse(practiceService.replaceEntryForCurrentUser(id, request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeEntryResponse> updatePracticeEntry(
            @PathVariable Long id,
            @RequestBody PracticeEntryRequest request
    ) {
        return ResponseEntity.ok(toResponse(practiceService.updateEntryForCurrentUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePracticeEntry(@PathVariable Long id) {
        practiceService.deleteEntryForCurrentUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSummaryDto> getSummary() {
        PracticeSummaryDto summary = practiceService.getSummaryForCurrentUser();
        return ResponseEntity.ok(summary);
    }

    private PracticeEntryResponse toResponse(PracticeEntry entry) {
        return new PracticeEntryResponse(
                entry.getId(),
                entry.getType(),
                entry.getMinutes(),
                entry.getDateTime()
        );
    }

    public record PracticeEntryResponse(
            Long id,
            PracticeType type,
            int minutes,
            LocalDateTime dateTime
    ) {
    }
}
