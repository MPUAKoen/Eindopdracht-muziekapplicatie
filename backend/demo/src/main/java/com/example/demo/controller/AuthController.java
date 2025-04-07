package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setInstrument(request.getInstrument()); // Set the instrument from the request
        user.setWorkingOnPieces(request.getWorkingOnPieces()); // Set the workingOnPieces from the request
        user.setRepertoire(request.getRepertoire()); // Set the repertoire from the request
        user.setWishlist(request.getWishlist()); // Set the wishlist from the request

        userRepository.save(user);
        return ResponseEntity.ok("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody RegistrationRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return ResponseEntity.ok("Login successful");
                    } else {
                        return ResponseEntity.badRequest().body("Invalid email or password");
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid email or password"));
    }

    public static class RegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String instrument;
        private List<String> workingOnPieces;
        private List<String> repertoire;
        private List<String> wishlist;

        // Getters and Setters
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
}
