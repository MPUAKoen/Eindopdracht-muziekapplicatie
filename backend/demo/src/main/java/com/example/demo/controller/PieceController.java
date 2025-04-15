package com.example.demo.controller;

import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/piece")
public class PieceController {

    private static final Logger logger = LoggerFactory.getLogger(PieceController.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @PostMapping("/add")
    public ResponseEntity<?> addPiece(@RequestBody PieceRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("User not authenticated.");
            return ResponseEntity.status(401).body("User not authenticated");
        }

        User currentUser;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            currentUser = (User) principal;
        } else if (principal instanceof String) {
            currentUser = userRepository.findByEmail((String) principal).orElse(null);
        } else {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        if (currentUser == null) {
            logger.warn("User not found.");
            return ResponseEntity.status(404).body("User not found");
        }

        // Create a new Piece with the submitted details.
        Piece newPiece = new Piece(request.getTitle(), request.getComposer(), request.getNotes());
        String category = request.getCategory().toLowerCase();

        // Add the piece to the proper collection.
        switch (category) {
            case "repertoire":
                List<Piece> repertoire = currentUser.getRepertoire();
                repertoire.add(newPiece);
                currentUser.setRepertoire(repertoire);
                break;
            case "wishlist":
                List<Piece> wishlist = currentUser.getWishlist();
                wishlist.add(newPiece);
                currentUser.setWishlist(wishlist);
                break;
            case "workingonpieces":
                List<Piece> workingOnPieces = currentUser.getWorkingOnPieces();
                workingOnPieces.add(newPiece);
                currentUser.setWorkingOnPieces(workingOnPieces);
                break;
            case "favorite":
                List<Piece> favoritePieces = currentUser.getFavoritePieces();
                favoritePieces.add(newPiece);
                currentUser.setFavoritePieces(favoritePieces);
                break;
            default:
                return ResponseEntity.badRequest()
                        .body("Invalid category. Valid options: repertoire, wishlist, workingonpieces, favorite");
        }

        userRepository.save(currentUser);
        logger.info("New piece added to {} for user {}", category, currentUser.getEmail());
        return ResponseEntity.ok("Piece added successfully");
    }

    @Transactional
    @PostMapping("/delete")
    public ResponseEntity<?> deletePiece(@RequestBody DeletePieceRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("User not authenticated.");
            return ResponseEntity.status(401).body("User not authenticated");
        }

        User currentUser;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            currentUser = (User) principal;
        } else if (principal instanceof String) {
            currentUser = userRepository.findByEmail((String) principal).orElse(null);
        } else {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        if (currentUser == null) {
            logger.warn("User not found.");
            return ResponseEntity.status(404).body("User not found");
        }

        String category = request.getCategory().toLowerCase();
        Piece pieceToDelete = new Piece(request.getTitle(), request.getComposer(), request.getNotes());
        boolean removed = false;

        switch (category) {
            case "repertoire":
                List<Piece> repertoire = currentUser.getRepertoire();
                removed = repertoire.removeIf(p -> pieceMatches(p, pieceToDelete));
                currentUser.setRepertoire(repertoire);
                break;
            case "wishlist":
                List<Piece> wishlist = currentUser.getWishlist();
                removed = wishlist.removeIf(p -> pieceMatches(p, pieceToDelete));
                currentUser.setWishlist(wishlist);
                break;
            case "workingonpieces":
                List<Piece> workingOnPieces = currentUser.getWorkingOnPieces();
                removed = workingOnPieces.removeIf(p -> pieceMatches(p, pieceToDelete));
                currentUser.setWorkingOnPieces(workingOnPieces);
                break;
            case "favorite":
                List<Piece> favoritePieces = currentUser.getFavoritePieces();
                removed = favoritePieces.removeIf(p -> pieceMatches(p, pieceToDelete));
                currentUser.setFavoritePieces(favoritePieces);
                break;
            default:
                return ResponseEntity.badRequest()
                        .body("Invalid category. Valid options: repertoire, wishlist, workingonpieces, favorite");
        }

        if (removed) {
            userRepository.save(currentUser);
            logger.info("Piece deleted from {} for user {}", category, currentUser.getEmail());
            return ResponseEntity.ok("Piece deleted successfully");
        } else {
            return ResponseEntity.status(404)
                    .body("Piece not found in user's " + category);
        }
    }

    private boolean pieceMatches(Piece p1, Piece p2) {
        return (p1.getTitle() != null ? p1.getTitle().equals(p2.getTitle()) : p2.getTitle() == null)
                && (p1.getComposer() != null ? p1.getComposer().equals(p2.getComposer()) : p2.getComposer() == null)
                && (p1.getNotes() != null ? p1.getNotes().equals(p2.getNotes()) : p2.getNotes() == null);
    }

    // DTO for piece data (used in the add endpoint)
    public static class PieceRequest {
        private String title;
        private String composer;
        private String notes;
        private String category; // expected: "repertoire", "wishlist", "workingonpieces", "favorite"

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getComposer() {
            return composer;
        }

        public void setComposer(String composer) {
            this.composer = composer;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    // DTO for piece deletion (used in the delete endpoint)
    public static class DeletePieceRequest {
        private String title;
        private String composer;
        private String notes;
        private String category; // expected: "repertoire", "wishlist", "workingonpieces", "favorite"

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getComposer() {
            return composer;
        }

        public void setComposer(String composer) {
            this.composer = composer;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
}
