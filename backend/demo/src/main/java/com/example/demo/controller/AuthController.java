package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // CURRENT USER ENDPOINT
    @GetMapping("/current")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(null);
        }
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new CurrentUserResponse(currentUser.getEmail(), currentUser.getName()));
    }

    // REGISTRATION ENDPOINT
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setInstrument(request.getInstrument());
        user.setWorkingOnPieces(request.getWorkingOnPieces());
        user.setRepertoire(request.getRepertoire());
        user.setWishlist(request.getWishlist());

        userRepository.save(user);
        return ResponseEntity.ok("Registration successful");
    }

    // LOGIN ENDPOINT
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        return ResponseEntity.ok("Login successful");
                    }
                    return ResponseEntity.badRequest().body("Invalid credentials");
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid credentials"));
    }

    // DTOs ================================================

    public static class CurrentUserResponse {
        private String email;
        private String name;

        public CurrentUserResponse() {
        }

        public CurrentUserResponse(String email, String name) {
            this.email = email;
            this.name = name;
        }

        // Getters/Setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class RegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String instrument;
        private List<String> workingOnPieces;
        private List<String> repertoire;
        private List<String> wishlist;

        // Getters/Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public List<String> getWorkingOnPieces() {
            return workingOnPieces;
        }

        public void setWorkingOnPieces(List<String> workingOnPieces) {
            this.workingOnPieces = workingOnPieces;
        }

        public List<String> getRepertoire() {
            return repertoire;
        }

        public void setRepertoire(List<String> repertoire) {
            this.repertoire = repertoire;
        }

        public List<String> getWishlist() {
            return wishlist;
        }

        public void setWishlist(List<String> wishlist) {
            this.wishlist = wishlist;
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        // Getters/Setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}