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

        // Auto-login after registration
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                newUser.getEmail(), null, newUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return ResponseEntity.ok(buildUserResponse(newUser));
    }

    // === SAFE Toggle User Role ===
    @PatchMapping("/toggle-role/{userId}")
    public ResponseEntity<?> toggleUserRole(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // === CASE 1: Demote a TEACHER to USER ===
        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            logger.info("Demoting teacher {} -> USER", user.getEmail());

            // Detach all students linked to this teacher
            List<User> allUsers = userRepository.findAll();
            for (User student : allUsers) {
                if (student.getTeacher() != null && student.getTeacher().getId().equals(user.getId())) {
                    student.setTeacher(null);
                    userRepository.save(student);
                }
            }

            // Demote teacher
            user.setRole("USER");
            userRepository.save(user);

            return ResponseEntity.ok("Teacher demoted to student.");
        }

        // === CASE 2: Promote a USER to TEACHER ===
        if ("USER".equalsIgnoreCase(user.getRole())) {
            logger.info("Promoting student {} -> TEACHER", user.getEmail());

            // If the student currently has a teacher, unlink them first
            if (user.getTeacher() != null) {
                User oldTeacher = user.getTeacher();
                oldTeacher.getUsers().remove(user); // ✅ correct
                user.setTeacher(null);
                userRepository.save(oldTeacher);
                userRepository.save(user);
            }

            // Promote to teacher
            user.setRole("TEACHER");
            userRepository.save(user);

            return ResponseEntity.ok("Student promoted to teacher.");
        }

        return ResponseEntity.badRequest().body("Invalid role transition");
    }

    // === Assign Teacher to Student ===
    @PatchMapping("/assign-teacher/{teacherId}")
    public ResponseEntity<?> assignTeacher(@PathVariable Long teacherId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String studentEmail = authentication.getName();
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        User teacher = userRepository.findById(teacherId)
                .filter(u -> "TEACHER".equalsIgnoreCase(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid teacher ID"));

        student.setTeacher(teacher);
        userRepository.save(student);

        logger.info("Assigned teacher {} to student {}", teacher.getId(), student.getId());
        return ResponseEntity.ok(buildUserResponse(student));
    }

    // === Delete User ===
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
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

    // === Get My Students (Assigned to Logged-In Teacher) ===
    @GetMapping("/my-students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<User>> getMyStudents(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String teacherEmail = authentication.getName();
        Optional<User> teacherOpt = userRepository.findByEmail(teacherEmail);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User teacher = teacherOpt.get();
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole().equalsIgnoreCase("USER"))
                .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacher.getId()))
                .toList();

        return ResponseEntity.ok(students);
    }

    // === Get All Teachers ===
    @GetMapping("/teachers")
    public ResponseEntity<List<User>> getTeachers() {
        logger.info("Fetching teachers.");
        List<User> teachers = userRepository.findByRole("TEACHER");
        if (teachers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(teachers);
    }

    // === Get Current User ===
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        logger.info("=== GET /api/user/current ===");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(buildUserResponse(currentUser));
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
            }
        }
        return ResponseEntity.badRequest().body("Invalid credentials");
    }

    // === DTOs ===
    public static class LoginRequest {
        private String email;
        private String password;

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

        public CurrentUserResponse(
                Long id,
                String name,
                String email,
                String instrument,
                String role,
                List<Piece> workingOnPieces,
                List<Piece> repertoire,
                List<Piece> wishlist,
                List<Piece> favoritePieces,
                TeacherDto teacher) {
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

    private CurrentUserResponse buildUserResponse(User user) {
        TeacherDto teacherDto = null;
        if (user.getTeacher() != null) {
            User t = user.getTeacher();
            teacherDto = new TeacherDto(
                    t.getId(), t.getName(), t.getEmail(), t.getInstrument());
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
                teacherDto);
    }
}
