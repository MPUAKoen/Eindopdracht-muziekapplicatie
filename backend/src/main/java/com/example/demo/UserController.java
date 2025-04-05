package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/user")  // Base URL for all user-related endpoints
@CrossOrigin(origins = "http://localhost:5173")  // Allow React frontend to interact with this API
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor-based dependency injection
    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Registration endpoint to handle POST requests for user registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto registrationDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        // Create a new User object and set the details from the DTO
        User user = new User();
        user.setName(registrationDto.getName());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));  // Encrypt password

        // Save the user to the database
        userRepository.save(user);

        // Return a success message
        return ResponseEntity.ok("Registration successful");
    }
}
