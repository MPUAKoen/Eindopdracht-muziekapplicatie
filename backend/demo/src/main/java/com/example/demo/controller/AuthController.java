package com.example.demo.controller;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already in use");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setInstrument(request.getInstrument());
        userRepository.save(newUser);

        logger.info("Registered new user: {}", newUser.getEmail());
        return ResponseEntity.ok(buildAuthResponse(newUser));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.info("Login successful: {}", user.getEmail());
                return ResponseEntity.ok(buildAuthResponse(user));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(buildUserResponse(requireCurrentUser(authentication)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        List<UserSummaryResponse> users = userRepository.findAll().stream()
                .map(this::buildUserSummaryResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id, Authentication authentication) {
        User currentUser = requireCurrentUser(authentication);
        User requestedUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = hasRole(currentUser, "ADMIN");
        boolean isSelf = currentUser.getId().equals(requestedUser.getId());
        boolean isAssignedStudent = hasRole(currentUser, "TEACHER")
                && hasRole(requestedUser, "USER")
                && requestedUser.getTeacher() != null
                && currentUser.getId().equals(requestedUser.getTeacher().getId());

        if (!isAdmin && !isSelf && !isAssignedStudent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own students");
        }

        return ResponseEntity.ok(buildUserResponse(requestedUser));
    }

    @GetMapping("/teachers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSummaryResponse>> getTeachers() {
        List<UserSummaryResponse> teachers = userRepository.findByRole("TEACHER").stream()
                .map(this::buildUserSummaryResponse)
                .toList();

        if (teachers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(teachers);
    }

    @PatchMapping("/toggle-role/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserRole(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            logger.info("Demoting teacher {}", user.getEmail());

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

        if ("USER".equalsIgnoreCase(user.getRole())) {
            logger.info("Promoting user {}", user.getEmail());

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

    @PatchMapping("/assign-teacher/{teacherId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> assignTeacher(@PathVariable Long teacherId, Authentication authentication) {
        User student = requireCurrentUser(authentication);
        User teacher = userRepository.findById(teacherId)
                .filter(u -> "TEACHER".equalsIgnoreCase(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid teacher ID"));

        student.setTeacher(teacher);
        userRepository.save(student);
        logger.info("Assigned teacher {} to student {}", teacher.getId(), student.getId());
        return ResponseEntity.ok(buildUserResponse(student));
    }

    @PatchMapping("/unassign-student/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> unassignStudent(@PathVariable Long studentId, Authentication authentication) {
        User teacher = requireCurrentUser(authentication);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only unassign your own students");
        }

        student.setTeacher(null);
        userRepository.save(student);
        logger.info("Teacher {} unassigned student {}", teacher.getEmail(), student.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<UserSummaryResponse>> getMyStudents(Authentication authentication) {
        User teacher = requireCurrentUser(authentication);

        List<UserSummaryResponse> students = userRepository.findAll().stream()
                .filter(u -> "USER".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacher.getId()))
                .map(this::buildUserSummaryResponse)
                .toList();

        return ResponseEntity.ok(students);
    }

    @DeleteMapping("/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logged out");
    }

    @PatchMapping("/update-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req, Authentication authentication) {
        User user = requireCurrentUser(authentication);

        if (req.getEmail() != null) {
            String newEmail = req.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                boolean taken = userRepository.findByEmail(newEmail).isPresent();
                if (taken) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already in use");
                }
                user.setEmail(newEmail);
            }
        }

        if (req.getName() != null) {
            user.setName(req.getName().trim());
        }
        if (req.getInstrument() != null) {
            user.setInstrument(req.getInstrument().trim());
        }

        userRepository.save(user);
        return ResponseEntity.ok(buildAuthResponse(user));
    }

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

    public static class UpdateProfileRequest {
        private String name;
        private String email;
        private String instrument;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
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

    public static class UserSummaryResponse {
        public Long id;
        public String name;
        public String email;
        public String instrument;
        public String role;

        public UserSummaryResponse(Long id, String name, String email, String instrument, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.instrument = instrument;
            this.role = role;
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
                TeacherDto teacher
        ) {
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

    public static class AuthResponse {
        public String token;
        public String tokenType;
        public long expiresIn;
        public CurrentUserResponse user;

        public AuthResponse(String token, String tokenType, long expiresIn, CurrentUserResponse user) {
            this.token = token;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.user = user;
        }
    }

    private CurrentUserResponse buildUserResponse(User user) {
        TeacherDto teacherDto = null;
        if (user.getTeacher() != null) {
            User teacher = user.getTeacher();
            teacherDto = new TeacherDto(teacher.getId(), teacher.getName(), teacher.getEmail(), teacher.getInstrument());
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

    private UserSummaryResponse buildUserSummaryResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getInstrument(),
                user.getRole()
        );
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                buildUserResponse(user)
        );
    }

    private User requireCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean hasRole(User user, String role) {
        return role.equalsIgnoreCase(user.getRole());
    }
}
