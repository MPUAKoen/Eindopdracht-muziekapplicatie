package com.example.demo.dto;

public record AuthResponse(String token, String tokenType, long expiresIn, CurrentUserResponse user) {
}
