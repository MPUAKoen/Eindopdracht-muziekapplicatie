package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}

@RestController
@RequestMapping("/api")
class HomepageController {

	private final List<Map<String, String>> workingOnPieces = new ArrayList<>(List.of(
			Map.of("title", "'Notte giorno faticar'", "focus", "Focus on dynamics and phrasing."),
			Map.of("title", "'Ein Mädchen oder Weibchen'", "focus", "Perfect the high notes."),
			Map.of("title", "'Pa-Pa-Papagena'", "focus", "Work on duet timing")
	));

	@GetMapping("/welcome")
	public Map<String, String> getWelcomeMessage() {
		return Map.of("message", "Welcome Back, Koen! Here’s a quick overview of your account.");
	}

	@GetMapping("/lessons")
	public List<Map<String, String>> getUpcomingLessons() {
		return List.of(
				Map.of("lesson", "Lesson 1", "time", "10:00 AM - 11:00 AM"),
				Map.of("lesson", "Lesson 2", "time", "2:00 PM - 3:00 PM")
		);
	}

	@GetMapping("/homework")
	public List<Map<String, String>> getRecentHomework() {
		return List.of(
				Map.of("date", "19-01", "assignment", "Complete your last assignment on 'Music Theory' to stay on track.")
		);
	}

	@GetMapping("/pieces")
	public List<Map<String, String>> getWorkingOnPieces() {
		return workingOnPieces;
	}

	@PostMapping("/pieces")
	public ResponseEntity<String> addPiece(@RequestBody Map<String, String> newPiece) {
		workingOnPieces.add(newPiece);
		return ResponseEntity.ok("Piece added successfully!");
	}
}
