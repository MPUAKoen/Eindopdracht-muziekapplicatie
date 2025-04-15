package com.example.demo.controller;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;
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
        System.out.println("=== GET /api/user/current ===");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + authentication);
        System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));

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
