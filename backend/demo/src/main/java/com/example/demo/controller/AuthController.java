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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles registration, authentication, teacher/student management,
 * and user CRUD endpoints for the music application.
 */
@RestController
@RequestMapping("/api/user")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // === Register ===
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

        logger.info("✅ Registered new user: {}", newUser.getEmail());

        // Auto-login after registration
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                newUser.getEmail(), null, newUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return ResponseEntity.ok(buildUserResponse(newUser));
    }

    // === Login ===
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

                logger.info("✅ Login successful: {}", user.getEmail());
                return ResponseEntity.ok(buildUserResponse(user));
            }
        }
        return ResponseEntity.badRequest().body("Invalid credentials");
    }

    // === Get current user ===
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(buildUserResponse(user));
    }

    // === Get all users ===
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // === Get specific user by ID (for teachers/admins to view student profile) ===
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(user);
    }

    // === Get all teachers ===
    @GetMapping("/teachers")
    public ResponseEntity<List<User>> getTeachers() {
        List<User> teachers = userRepository.findByRole("TEACHER");
        if (teachers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(teachers);
    }

    // === Toggle role (promote/demote) ===
    @PatchMapping("/toggle-role/{userId}")
    public ResponseEntity<?> toggleUserRole(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // --- Demote TEACHER → USER
        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            logger.info("⬇️ Demoting teacher {}", user.getEmail());

            // Detach all students from this teacher
            userRepository.findAll().forEach(student -> {
                if (student.getTeacher() != null && student.getTeacher().getId().equals(user.getId())) {
                    student.setTeacher(null);
                    userRepository.save(student);
                }
            });

            user.setRole("USER");
            userRepository.save(user);
            return ResponseEntity.ok("Teacher demoted to USER");
        }

        // --- Promote USER → TEACHER
        if ("USER".equalsIgnoreCase(user.getRole())) {
            logger.info("⬆️ Promoting user {}", user.getEmail());

            // Unlink from old teacher if exists
            if (user.getTeacher() != null) {
                User oldTeacher = user.getTeacher();
                oldTeacher.getUsers().remove(user);
                user.setTeacher(null);
                userRepository.save(oldTeacher);
            }

            user.setRole("TEACHER");
            userRepository.save(user);
            return ResponseEntity.ok("User promoted to TEACHER");
        }

        return ResponseEntity.badRequest().body("Invalid role transition");
    }

    // === Assign teacher to a student ===
    @PatchMapping("/assign-teacher/{teacherId}")
    public ResponseEntity<?> assignTeacher(@PathVariable Long teacherId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");

        String studentEmail = authentication.getName();
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        User teacher = userRepository.findById(teacherId)
                .filter(u -> "TEACHER".equalsIgnoreCase(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid teacher ID"));

        student.setTeacher(teacher);
        userRepository.save(student);
        logger.info("👩‍🏫 Assigned teacher {} to student {}", teacher.getId(), student.getId());
        return ResponseEntity.ok(buildUserResponse(student));
    }

    // === Unassign a student from the logged-in teacher ===
    @PatchMapping("/unassign-student/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> unassignStudent(@PathVariable Long studentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");

        String teacherEmail = authentication.getName();
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only unassign your own students");
        }

        student.setTeacher(null);
        userRepository.save(student);
        logger.info("🗑️ Teacher {} unassigned student {}", teacher.getEmail(), student.getEmail());
        return ResponseEntity.noContent().build();
    }

    // === Get my students (logged-in teacher) ===
    @GetMapping("/my-students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<User>> getMyStudents(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String teacherEmail = authentication.getName();
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        List<User> students = userRepository.findAll().stream()
                .filter(u -> "USER".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacher.getId()))
                .toList();

        return ResponseEntity.ok(students);
    }

    // === Delete user ===
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        if (userRepository.findById(userId).isEmpty()) return ResponseEntity.notFound().build();
        userRepository.deleteById(userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    // === DTOs ===
    public static class LoginRequest {
        private String email;
        private String password;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
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

    public static class TeacherDto {
        public Long id;
        public String name;
        public String email;
        public String instrument;
        public TeacherDto(Long id, String name, String email, String instrument) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.instrument = instrument;
        }
    }

    public static class CurrentUserResponse {
        public Long id;
        public String name;
        public String email;
        public String instrument;
        public String role;
        public List<Piece> workingOnPieces;
        public List<Piece> repertoire;
        public List<Piece> wishlist;
        public List<Piece> favoritePieces;
        public TeacherDto teacher;

        public CurrentUserResponse(Long id, String name, String email, String instrument, String role,
                                   List<Piece> workingOnPieces, List<Piece> repertoire,
                                   List<Piece> wishlist, List<Piece> favoritePieces, TeacherDto teacher) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.instrument = instrument;
            this.role = role;
            this.workingOnPieces = workingOnPieces;
            this.repertoire = repertoire;
            this.wishlist = wishlist;
            this.favoritePieces = favoritePieces;
            this.teacher = teacher;
        }
    }

    // === Helper for building JSON responses ===
    private CurrentUserResponse buildUserResponse(User user) {
        TeacherDto teacherDto = null;
        if (user.getTeacher() != null) {
            User t = user.getTeacher();
            teacherDto = new TeacherDto(t.getId(), t.getName(), t.getEmail(), t.getInstrument());
        }
        return new CurrentUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getInstrument(),
                user.getRole(),
                user.getWorkingOnPieces(),
                user.getRepertoire(),
                user.getWishlist(),
                user.getFavoritePieces(),
                teacherDto
        );
    }
}
