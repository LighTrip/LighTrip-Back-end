package com.kauniv.lightrip.domain.like.repository;

import com.kauniv.lightrip.domain.like.entity.Like;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    @Modifying
    @Query("DELETE FROM Like l WHERE l.passport.id = :passportId")
    void deleteAllByPassportId(Long passportId);

    boolean existsByUser_IdAndPassport_Id(Long userId, Long passportId);

    Optional<Like> findByUser_IdAndPassport_Id(Long userId, Long passportId);

    @Query("""
            SELECT l FROM Like l
            JOIN FETCH l.passport p
            LEFT JOIN FETCH p.images
            WHERE l.user.id = :userId
            ORDER BY l.id DESC
            """)
    List<Like> findByUserIdFirst(Long userId, Pageable pageable);

    @Query("""
            SELECT l FROM Like l
            JOIN FETCH l.passport p
            LEFT JOIN FETCH p.images
            WHERE l.user.id = :userId AND l.id < :cursor
            ORDER BY l.id DESC
            """)
    List<Like> findByUserIdAfterCursor(Long userId, Long cursor, Pageable pageable);

    long countByUser_Id(Long userId);
}