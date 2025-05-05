package com.example.demo.controller;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // === Register User ===
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already in use");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setInstrument(request.getInstrument());

        userRepository.save(newUser);
        logger.info("User registered successfully");

        // Optional: Auto-login after registration
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                newUser.getEmail(), null, newUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return ResponseEntity.ok(buildUserResponse(newUser));
    }

    // === Toggle User Role (User -> Teacher or Teacher -> User) ===
    @PatchMapping("/toggle-role/{userId}")
    public ResponseEntity<?> toggleUserRole(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        String newRole = user.getRole().equalsIgnoreCase("TEACHER") ? "USER" : "TEACHER";
        user.setRole(newRole);
        userRepository.save(user);

        return ResponseEntity.ok("User role updated to " + newRole);
    }

    // === Delete User ===
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    // === Get All Users ===
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // === Get All Teachers ===
    @GetMapping("/teachers")
    public ResponseEntity<List<User>> getTeachers() {
        // Log the request to ensure the method is being called
        logger.info("Fetching teachers...");

        // Fetch users with the role "TEACHER"
        List<User> teachers = userRepository.findByRole("TEACHER");

        // Log the result to check if it's populated correctly
        logger.info("Teachers found: " + teachers.size());

        if (teachers.isEmpty()) {
            return ResponseEntity.noContent().build(); // No teachers found
        }

        // Add headers to prevent caching of this response
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0") // Ensure the response is not cached
                .body(teachers); // Return the list of teachers
    }

    // === Get Current User ===
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        logger.info("=== GET /api/user/current ===");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authentication: " + authentication);
        logger.info("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        User currentUser = null;

        if (principal instanceof User) {
            currentUser = (User) principal;
        } else if (principal instanceof String) {
            currentUser = userRepository.findByEmail((String) principal).orElse(null);
        }

        if (currentUser == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        return ResponseEntity.ok(new CurrentUserResponse(
                currentUser.getId(),
                currentUser.getName(),
                currentUser.getEmail(),
                currentUser.getInstrument(),
                currentUser.getWorkingOnPieces(),
                currentUser.getRepertoire(),
                currentUser.getWishlist(),
                currentUser.getFavoritePieces()));
    }

    // === Login User ===
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, user.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

                logger.info("User logged in successfully");

                return ResponseEntity.ok(buildUserResponse(user));
            } else {
                return ResponseEntity.badRequest().body("Invalid credentials");
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid credentials");
        }
    }

    // === Utility Classes ===

    public static class CurrentUserResponse {
        public Long id;
        public String name;
        public String email;
        public String instrument;
        public List<Piece> workingOnPieces, repertoire, wishlist, favoritePieces;

        public CurrentUserResponse(Long id, String name, String email, String instrument,
                List<Piece> workingOnPieces, List<Piece> repertoire,
                List<Piece> wishlist, List<Piece> favoritePieces) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.instrument = instrument;
            this.workingOnPieces = workingOnPieces;
            this.repertoire = repertoire;
            this.wishlist = wishlist;
            this.favoritePieces = favoritePieces;
        }
    }

    public static class LoginRequest {
        private String email, password;

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

    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private String instrument;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getInstrument() { return instrument; }
        public void setInstrument(String instrument) { this.instrument = instrument; }
    }

    private CurrentUserResponse buildUserResponse(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getInstrument(),
                user.getWorkingOnPieces(),
                user.getRepertoire(),
                user.getWishlist(),
                user.getFavoritePieces());
    }
}
