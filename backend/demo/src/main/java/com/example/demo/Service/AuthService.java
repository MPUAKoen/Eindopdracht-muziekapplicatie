package com.example.demo.Service;

import com.example.demo.dto.AdminUserUpdateRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.CurrentUserResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.PieceResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.TeacherAssignmentRequest;
import com.example.demo.dto.TeacherDto;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserSummaryResponse;
import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityManager entityManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
    }

    public AuthResponse register(RegisterRequest request) {
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
        return buildAuthResponse(newUser);
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().trim());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.info("Login successful: {}", user.getEmail());
                return buildAuthResponse(user);
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    public CurrentUserResponse getCurrentUser(Authentication authentication) {
        return buildUserResponse(requireCurrentUser(authentication));
    }

    public AuthResponse updateProfile(UpdateProfileRequest request, Authentication authentication, boolean replace) {
        User user = requireCurrentUser(authentication);
        if (replace) {
            requireUserProfilePayload(request);
        }
        applyUserFields(user, request);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::buildUserSummaryResponse)
                .toList();
    }

    public CurrentUserResponse getUserById(Long id, Authentication authentication) {
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

        return buildUserResponse(requestedUser);
    }

    public UserSummaryResponse updateUser(Long userId, AdminUserUpdateRequest request, boolean replace) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User payload is required");
        }
        if (replace) {
            requireAdminUserPayload(request);
        }

        applyUserFields(user, request);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            updateRole(user, request.getRole());
        }

        userRepository.save(user);
        return buildUserSummaryResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        logger.info("Admin deleting user {} ({})", user.getId(), user.getEmail());
        deleteUserDependencies(userId);
        entityManager.clear();
        executeNativeUpdate("delete from users where id = :userId", userId);
    }

    public List<UserSummaryResponse> getTeachers() {
        return userRepository.findByRole("TEACHER").stream()
                .map(this::buildUserSummaryResponse)
                .toList();
    }

    public CurrentUserResponse assignTeacher(TeacherAssignmentRequest request, Authentication authentication) {
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
        return buildUserResponse(student);
    }

    public CurrentUserResponse unassignMyTeacher(Authentication authentication) {
        User student = requireCurrentUser(authentication);
        student.setTeacher(null);
        userRepository.save(student);
        return buildUserResponse(student);
    }

    public List<UserSummaryResponse> getMyStudents(Authentication authentication) {
        User teacher = requireCurrentUser(authentication);

        return userRepository.findAll().stream()
                .filter(u -> hasRole(u, "USER"))
                .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacher.getId()))
                .map(this::buildUserSummaryResponse)
                .toList();
    }

    public void unassignStudent(Long studentId, Authentication authentication) {
        User teacher = requireCurrentUser(authentication);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only unassign your own students");
        }

        student.setTeacher(null);
        userRepository.save(student);
        logger.info("Teacher {} unassigned student {}", teacher.getEmail(), student.getEmail());
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
                mapPieces(user.getWorkingOnPieces(), "working-on-pieces"),
                mapPieces(user.getRepertoire(), "repertoire"),
                mapPieces(user.getWishlist(), "wishlist"),
                mapPieces(user.getFavoritePieces(), "favorite"),
                teacherDto
        );
    }

    private List<PieceResponse> mapPieces(List<Piece> pieces, String category) {
        if (pieces == null) {
            return List.of();
        }
        return pieces.stream()
                .filter(piece -> piece != null)
                .map(piece -> new PieceResponse(
                        piece.getId(),
                        piece.getTitle(),
                        piece.getComposer(),
                        piece.getNotes(),
                        category
                ))
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

    private void deleteUserDependencies(Long userId) {
        executeNativeUpdate("update users set teacher_id = null where teacher_id = :userId", userId);
        executeNativeUpdate("""
                delete from lesson_files
                where lesson_id in (
                    select id
                    from lessons
                    where teacher_id = :userId
                       or student_id = :userId
                )
                """, userId);
        executeNativeUpdate("delete from lessons where teacher_id = :userId or student_id = :userId", userId);
        executeNativeUpdate("delete from practice_entry where practice_log_id = :userId", userId);
        executeNativeUpdate("delete from practice_log where user_id = :userId", userId);

        for (String tableName : List.of(
                "user_favorite_piece_links",
                "user_wishlist_piece_links",
                "user_working_piece_links",
                "user_repertoire_piece_links"
        )) {
            executeNativeUpdate("delete from " + tableName + " where user_id = :userId", userId);
        }
    }

    private void executeNativeUpdate(String sql, Long userId) {
        entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .executeUpdate();
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

        if (request.getPassword() != null) {
            String password = request.getPassword().trim();
            if (password.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be blank");
            }
            if (password.length() < 6 || password.length() > 72) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be between 6 and 72 characters");
            }
            user.setPassword(passwordEncoder.encode(password));
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
