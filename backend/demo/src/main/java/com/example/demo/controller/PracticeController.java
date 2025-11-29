package com.example.demo.controller;

import com.example.demo.Service.PracticeService;
import com.example.demo.dto.PracticeEntryRequest;
import com.example.demo.dto.PracticeSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addPracticeEntry(@RequestBody PracticeEntryRequest request) {
        practiceService.addEntryForCurrentUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<PracticeSummaryDto> getSummary() {
        PracticeSummaryDto summary = practiceService.getSummaryForCurrentUser();
        return ResponseEntity.ok(summary);
    }
}
