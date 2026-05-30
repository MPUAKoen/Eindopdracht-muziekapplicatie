package com.example.demo.Service;

import com.example.demo.dto.PieceRequest;
import com.example.demo.dto.PieceResponse;
import com.example.demo.model.Piece;
import com.example.demo.model.User;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PieceService {

    private static final Logger logger = LoggerFactory.getLogger(PieceService.class);

    private final UserRepository userRepository;
    private final PieceRepository pieceRepository;
    private final EntityManager entityManager;

    public PieceService(UserRepository userRepository, PieceRepository pieceRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.pieceRepository = pieceRepository;
        this.entityManager = entityManager;
    }

    public List<PieceResponse> getPieces(String category, Authentication authentication) {
        User user = requireCurrentUser(authentication);
        String normalizedCategory = normalizeCategory(category);
        return getList(user, normalizedCategory).stream()
                .filter(piece -> piece != null)
                .map(piece -> toResponse(piece, normalizedCategory))
                .toList();
    }

    public PieceResponse getPiece(Long pieceId, Authentication authentication) {
        User user = requireCurrentUser(authentication);
        LocatedPiece locatedPiece = findPiece(user, pieceId);
        return toResponse(locatedPiece.piece(), locatedPiece.category());
    }

    public PieceResponse addPiece(PieceRequest request, Authentication authentication) {
        User user = requireCurrentUser(authentication);
        String category = normalizeCategory(request.getCategory());

        Piece piece = new Piece(
                requireText(request.getTitle(), "title"),
                requireText(request.getComposer(), "composer"),
                trimToNull(request.getNotes())
        );

        getList(user, category).add(piece);
        User savedUser = userRepository.saveAndFlush(user);
        List<Piece> savedPieces = getList(savedUser, category);
        Piece savedPiece = savedPieces.get(savedPieces.size() - 1);
        return toResponse(savedPiece, category);
    }

    public PieceResponse updatePiece(Long pieceId, PieceRequest request, Authentication authentication) {
        User user = requireCurrentUser(authentication);
        LocatedPiece locatedPiece = findPiece(user, pieceId);
        Piece piece = locatedPiece.piece();
        String category = locatedPiece.category();

        if (request.getTitle() != null) {
            piece.setTitle(requireText(request.getTitle(), "title"));
        }
        if (request.getComposer() != null) {
            piece.setComposer(requireText(request.getComposer(), "composer"));
        }
        if (request.getNotes() != null) {
            piece.setNotes(trimToNull(request.getNotes()));
        }
        if (request.getCategory() != null) {
            String targetCategory = normalizeCategory(request.getCategory());
            if (!targetCategory.equals(category)) {
                locatedPiece.list().removeIf(existingPiece -> pieceId.equals(existingPiece.getId()));
                getList(user, targetCategory).add(piece);
                category = targetCategory;
            }
        }

        userRepository.save(user);
        return toResponse(piece, category);
    }

    @Transactional
    public void deletePiece(Long pieceId, Authentication authentication) {
        CurrentUserRef user = requireCurrentUserRef(authentication);
        String category = findLinkedCategory(user.id(), pieceId);

        logger.info("Deleting piece {} from {} category for user {}", pieceId, category, user.email());

        deleteCategoryLink(category, user.id(), pieceId);
        compactCategoryOrder(category, user.id());
        entityManager.clear();
    }

    private User requireCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private CurrentUserRef requireCurrentUserRef(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select id, email
                from users
                where email = :email
                """)
                .setParameter("email", authentication.getName())
                .getResultList();

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Object[] row = rows.get(0);
        return new CurrentUserRef(((Number) row[0]).longValue(), (String) row[1]);
    }

    private LocatedPiece findPiece(User user, Long pieceId) {
        for (String category : List.of("favorite", "wishlist", "working-on-pieces", "repertoire")) {
            List<Piece> list = getList(user, category);
            for (Piece piece : list) {
                if (piece != null && pieceId.equals(piece.getId())) {
                    return new LocatedPiece(category, list, piece);
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Piece not found");
    }

    private List<Piece> getList(User user, String category) {
        return switch (category) {
            case "favorite" -> user.getFavoritePieces();
            case "wishlist" -> user.getWishlist();
            case "working-on-pieces" -> user.getWorkingOnPieces();
            case "repertoire" -> user.getRepertoire();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        };
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }

        return switch (category.trim().toLowerCase()) {
            case "favorite", "favorite-pieces" -> "favorite";
            case "wishlist" -> "wishlist";
            case "workingonpieces", "working-on-pieces" -> "working-on-pieces";
            case "repertoire" -> "repertoire";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        };
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void deleteCategoryLink(String category, Long userId, Long pieceId) {
        switch (category) {
            case "favorite" -> pieceRepository.deleteFavoritePieceLink(userId, pieceId);
            case "wishlist" -> pieceRepository.deleteWishlistPieceLink(userId, pieceId);
            case "working-on-pieces" -> pieceRepository.deleteWorkingPieceLink(userId, pieceId);
            case "repertoire" -> pieceRepository.deleteRepertoirePieceLink(userId, pieceId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        }
    }

    private String findLinkedCategory(Long userId, Long pieceId) {
        for (String category : List.of("favorite", "wishlist", "working-on-pieces", "repertoire")) {
            String tableName = tableNameForCategory(category);
            Number count = (Number) entityManager.createNativeQuery("""
                    select count(*)
                    from %s
                    where user_id = :userId
                      and piece_id = :pieceId
                    """.formatted(tableName))
                    .setParameter("userId", userId)
                    .setParameter("pieceId", pieceId)
                    .getSingleResult();

            if (count.longValue() > 0) {
                return category;
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Piece not found");
    }

    private void compactCategoryOrder(String category, Long userId) {
        String tableName = tableNameForCategory(category);

        entityManager.createNativeQuery("""
                update %s
                set piece_order = -piece_order - 1
                where user_id = :userId
                """.formatted(tableName))
                .setParameter("userId", userId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                update %s links
                set piece_order = ranked.new_order
                from (
                    select user_id,
                           piece_id,
                           row_number() over (partition by user_id order by piece_order desc) - 1 as new_order
                    from %s
                    where user_id = :userId
                ) ranked
                where links.user_id = ranked.user_id
                  and links.piece_id = ranked.piece_id
                """.formatted(tableName, tableName))
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private String tableNameForCategory(String category) {
        return switch (category) {
            case "favorite" -> "user_favorite_piece_links";
            case "wishlist" -> "user_wishlist_piece_links";
            case "working-on-pieces" -> "user_working_piece_links";
            case "repertoire" -> "user_repertoire_piece_links";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        };
    }

    private PieceResponse toResponse(Piece piece, String category) {
        return new PieceResponse(
                piece.getId(),
                piece.getTitle(),
                piece.getComposer(),
                piece.getNotes(),
                category
        );
    }

    private record LocatedPiece(String category, List<Piece> list, Piece piece) {
    }

    private record CurrentUserRef(Long id, String email) {
    }
}
