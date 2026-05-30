package com.example.demo.controller;

import com.example.demo.Service.AuthService;
import com.example.demo.dto.AdminUserUpdateRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.CurrentUserResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.TeacherAssignmentRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication));
    }

    @PatchMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.updateProfile(request, authentication, false));
    }

    @PutMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> replaceProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.updateProfile(request, authentication, true));
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/api/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(authService.getUserById(id, authentication));
    }

    @PatchMapping("/api/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ResponseEntity.ok(authService.updateUser(userId, request, false));
    }

    @PutMapping("/api/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> replaceUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ResponseEntity.ok(authService.updateUser(userId, request, true));
    }

    @DeleteMapping("/api/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/teachers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSummaryResponse>> getTeachers() {
        return ResponseEntity.ok(authService.getTeachers());
    }

    @PutMapping("/api/users/me/teacher")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CurrentUserResponse> assignTeacher(
            @Valid @RequestBody TeacherAssignmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.assignTeacher(request, authentication));
    }

    @DeleteMapping("/api/users/me/teacher")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CurrentUserResponse> unassignMyTeacher(Authentication authentication) {
        return ResponseEntity.ok(authService.unassignMyTeacher(authentication));
    }

    @GetMapping("/api/teachers/me/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<UserSummaryResponse>> getMyStudents(Authentication authentication) {
        return ResponseEntity.ok(authService.getMyStudents(authentication));
    }

    @DeleteMapping("/api/teachers/me/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> unassignStudent(@PathVariable Long studentId, Authentication authentication) {
        authService.unassignStudent(studentId, authentication);
        return ResponseEntity.noContent().build();
    }
}
