package com.example.demo.controller;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LessonRepository lessonRepository;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LessonRepository lessonRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.lessonRepository = lessonRepository;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration payload is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        User newUser = new User();
        newUser.setName(request.getName().trim());
        newUser.setEmail(request.getEmail().trim());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setInstrument(trimToNull(request.getInstrument()));
        userRepository.save(newUser);

        logger.info("Registered new user: {}", newUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(newUser));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().trim());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.info("Login successful: {}", user.getEmail());
                return ResponseEntity.ok(buildAuthResponse(user));
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(buildUserResponse(requireCurrentUser(authentication)));
    }

    @PatchMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        User user = requireCurrentUser(authentication);
        applyUserFields(user, request);
        userRepository.save(user);
        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PutMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> replaceProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        User user = requireCurrentUser(authentication);
        requireUserProfilePayload(request);
        applyUserFields(user, request);
        userRepository.save(user);
        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        List<UserSummaryResponse> users = userRepository.findAll().stream()
                .map(this::buildUserSummaryResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/api/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view yourself or your own students");
        }

        return ResponseEntity.ok(buildUserResponse(requestedUser));
    }

    @PatchMapping("/api/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody AdminUserUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User payload is required");
        }

        applyUserFields(user, request);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            updateRole(user, request.getRole());
        }

        userRepository.save(user);
        return ResponseEntity.ok(buildUserSummaryResponse(user));
    }

    @PutMapping("/api/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> replaceUser(
            @PathVariable Long userId,
            @RequestBody AdminUserUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        requireAdminUserPayload(request);
        applyUserFields(user, request);
        updateRole(user, request.getRole());

        userRepository.save(user);
        return ResponseEntity.ok(buildUserSummaryResponse(user));
    }

    @DeleteMapping("/api/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (hasRole(user, "TEACHER")) {
            userRepository.findAll().stream()
                    .filter(student -> student.getTeacher() != null && userId.equals(student.getTeacher().getId()))
                    .forEach(student -> {
                        student.setTeacher(null);
                        userRepository.save(student);
                    });
        }

        lessonRepository.deleteAll(lessonRepository.findByTeacher(user));
        lessonRepository.deleteAll(lessonRepository.findByStudent(user));
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/teachers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSummaryResponse>> getTeachers() {
        List<UserSummaryResponse> teachers = userRepository.findByRole("TEACHER").stream()
                .map(this::buildUserSummaryResponse)
                .toList();
        return ResponseEntity.ok(teachers);
    }

    @PutMapping("/api/users/me/teacher")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CurrentUserResponse> assignTeacher(
            @RequestBody TeacherAssignmentRequest request,
            Authentication authentication
    ) {
        if (request == null || request.getTeacherId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teacherId is required");
        }

        User student = requireCurrentUser(authentication);
        User teacher = userRepository.findById(request.getTeacherId())
                .filter(u -> hasRole(u, "TEACHER"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid teacher ID"));

        student.setTeacher(teacher);
        userRepository.save(student);
        logger.info("Assigned teacher {} to student {}", teacher.getId(), student.getId());
        return ResponseEntity.ok(buildUserResponse(student));
    }

    @DeleteMapping("/api/users/me/teacher")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CurrentUserResponse> unassignMyTeacher(Authentication authentication) {
        User student = requireCurrentUser(authentication);
        student.setTeacher(null);
        userRepository.save(student);
        return ResponseEntity.ok(buildUserResponse(student));
    }

    @GetMapping("/api/teachers/me/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<UserSummaryResponse>> getMyStudents(Authentication authentication) {
        User teacher = requireCurrentUser(authentication);

        List<UserSummaryResponse> students = userRepository.findAll().stream()
                .filter(u -> hasRole(u, "USER"))
                .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacher.getId()))
                .map(this::buildUserSummaryResponse)
                .toList();

        return ResponseEntity.ok(students);
    }

    @DeleteMapping("/api/teachers/me/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> unassignStudent(@PathVariable Long studentId, Authentication authentication) {
        User teacher = requireCurrentUser(authentication);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only unassign your own students");
        }

        student.setTeacher(null);
        userRepository.save(student);
        logger.info("Teacher {} unassigned student {}", teacher.getEmail(), student.getEmail());
        return ResponseEntity.noContent().build();
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

    public static class AdminUserUpdateRequest extends UpdateProfileRequest {
        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class TeacherAssignmentRequest {
        private Long teacherId;

        public Long getTeacherId() {
            return teacherId;
        }

        public void setTeacherId(Long teacherId) {
            this.teacherId = teacherId;
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
                safePieces(user.getWorkingOnPieces()),
                safePieces(user.getRepertoire()),
                safePieces(user.getWishlist()),
                safePieces(user.getFavoritePieces()),
                teacherDto
        );
    }

    private List<Piece> safePieces(List<Piece> pieces) {
        if (pieces == null) {
            return List.of();
        }
        return pieces.stream()
                .filter(piece -> piece != null)
                .toList();
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

    private void applyUserFields(User user, UpdateProfileRequest request) {
        if (request == null) {
            return;
        }

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (newEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be blank");
            }
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                boolean taken = userRepository.findByEmail(newEmail).isPresent();
                if (taken) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
                }
                user.setEmail(newEmail);
            }
        }

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be blank");
            }
            user.setName(name);
        }

        if (request.getInstrument() != null) {
            user.setInstrument(trimToNull(request.getInstrument()));
        }
    }

    private void requireUserProfilePayload(UpdateProfileRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User payload is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
    }

    private void requireAdminUserPayload(AdminUserUpdateRequest request) {
        requireUserProfilePayload(request);
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
    }

    private void updateRole(User user, String requestedRole) {
        String targetRole = requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!List.of("USER", "TEACHER", "ADMIN").contains(targetRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        if (targetRole.equalsIgnoreCase(user.getRole())) {
            return;
        }

        if ("TEACHER".equalsIgnoreCase(user.getRole()) && !"TEACHER".equals(targetRole)) {
            userRepository.findAll().forEach(student -> {
                if (student.getTeacher() != null && student.getTeacher().getId().equals(user.getId())) {
                    student.setTeacher(null);
                    userRepository.save(student);
                }
            });
        }

        if ("USER".equalsIgnoreCase(user.getRole()) && "TEACHER".equals(targetRole) && user.getTeacher() != null) {
            user.setTeacher(null);
        }

        user.setRole(targetRole);
    }

    private boolean hasRole(User user, String role) {
        return role.equalsIgnoreCase(user.getRole());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
