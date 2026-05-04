package com.example.demo.repository;

import com.example.demo.model.Piece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PieceRepository extends JpaRepository<Piece, Long> {

    @Modifying
    @Query(value = "delete from user_working_piece_links where user_id = :userId and piece_id = :pieceId", nativeQuery = true)
    void deleteWorkingPieceLink(@Param("userId") Long userId, @Param("pieceId") Long pieceId);

    @Modifying
    @Query(value = "delete from user_repertoire_piece_links where user_id = :userId and piece_id = :pieceId", nativeQuery = true)
    void deleteRepertoirePieceLink(@Param("userId") Long userId, @Param("pieceId") Long pieceId);

    @Modifying
    @Query(value = "delete from user_wishlist_piece_links where user_id = :userId and piece_id = :pieceId", nativeQuery = true)
    void deleteWishlistPieceLink(@Param("userId") Long userId, @Param("pieceId") Long pieceId);

    @Modifying
    @Query(value = "delete from user_favorite_piece_links where user_id = :userId and piece_id = :pieceId", nativeQuery = true)
    void deleteFavoritePieceLink(@Param("userId") Long userId, @Param("pieceId") Long pieceId);
}
