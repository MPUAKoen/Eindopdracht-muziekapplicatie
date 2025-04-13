package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // Retrieve the authentication from SecurityContextHolder
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.info("GET /api/user/current called. Authentication: {}", authentication);

            // Check if the user is authenticated and the principal is a User instance
            if (authentication == null || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof User)) {
                Object principal = authentication != null ? authentication.getPrincipal() : null;
                logger.warn("User not authenticated or principal is not a User instance. Principal: {}", principal);
                return ResponseEntity.status(401).body("User not authenticated");
            }

            // Cast the principal and build the response DTO
            User currentUser = (User) authentication.getPrincipal();
            return ResponseEntity.ok(new CurrentUserResponse(
                    currentUser.getId(),
                    currentUser.getName(),
                    currentUser.getEmail(),
                    currentUser.getInstrument(),
                    currentUser.getWorkingOnPieces(),
                    currentUser.getRepertoire(),
                    currentUser.getWishlist()));
        } catch (Exception e) {
            logger.error("Error in getCurrentUser: ", e);
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

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

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities());
                        // Set the authentication into the SecurityContextHolder
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        // Explicitly create a session and save the SecurityContext into it
                        HttpSession session = httpRequest.getSession(true);
                        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                        logger.info("User logged in successfully; session ID: {}", session.getId());
                        return ResponseEntity.ok("Login successful");
                    }
                    return ResponseEntity.badRequest().body("Invalid credentials");
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid credentials"));
    }

    // DTO Classes

    public static class CurrentUserResponse {
        private Long id;
        private String name;
        private String email;
        private String instrument;
        private List<String> workingOnPieces;
        private List<String> repertoire;
        private List<String> wishlist;

        public CurrentUserResponse() {
        }

        public CurrentUserResponse(Long id, String name, String email, String instrument,
                List<String> workingOnPieces, List<String> repertoire,
                List<String> wishlist) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.instrument = instrument;
            this.workingOnPieces = workingOnPieces;
            this.repertoire = repertoire;
            this.wishlist = wishlist;
        }

        // Getters and setters

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

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

    public static class RegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String instrument;
        private List<String> workingOnPieces;
        private List<String> repertoire;
        private List<String> wishlist;

        // Getters and setters

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

        // Getters and setters

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
