package com.example.demo.controller;

import com.example.demo.Service.PieceService;
import com.example.demo.dto.CompletePiece;
import com.example.demo.dto.PieceRequest;
import com.example.demo.dto.PieceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pieces")
@PreAuthorize("isAuthenticated()")
public class PieceController {

    private final PieceService pieceService;

    public PieceController(PieceService pieceService) {
        this.pieceService = pieceService;
    }

    @GetMapping
    public ResponseEntity<List<PieceResponse>> getPieces(
            @RequestParam String category,
            Authentication authentication
    ) {
        return ResponseEntity.ok(pieceService.getPieces(category, authentication));
    }

    @GetMapping("/{pieceId}")
    public ResponseEntity<PieceResponse> getPiece(@PathVariable Long pieceId, Authentication authentication) {
        return ResponseEntity.ok(pieceService.getPiece(pieceId, authentication));
    }

    @PostMapping
    public ResponseEntity<PieceResponse> addPiece(
            @Validated(CompletePiece.class) @RequestBody PieceRequest request,
            Authentication authentication
    ) {
        PieceResponse savedPiece = pieceService.addPiece(request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedPiece.id())
                .toUri();

        return ResponseEntity.created(location).body(savedPiece);
    }

    @PutMapping("/{pieceId}")
    public ResponseEntity<PieceResponse> updatePiece(
            @PathVariable Long pieceId,
            @Validated(CompletePiece.class) @RequestBody PieceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(pieceService.updatePiece(pieceId, request, authentication));
    }

    @PatchMapping("/{pieceId}")
    public ResponseEntity<PieceResponse> patchPiece(
            @PathVariable Long pieceId,
            @Validated @RequestBody PieceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(pieceService.updatePiece(pieceId, request, authentication));
    }

    @DeleteMapping("/{pieceId}")
    public ResponseEntity<Void> deletePiece(@PathVariable Long pieceId, Authentication authentication) {
        pieceService.deletePiece(pieceId, authentication);
        return ResponseEntity.noContent().build();
    }
}
