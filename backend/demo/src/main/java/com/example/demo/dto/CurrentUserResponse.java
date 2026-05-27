package com.example.demo.dto;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String name,
        String email,
        String instrument,
        String role,
        List<PieceResponse> workingOnPieces,
        List<PieceResponse> repertoire,
        List<PieceResponse> wishlist,
        List<PieceResponse> favoritePieces,
        TeacherDto teacher
) {
}
