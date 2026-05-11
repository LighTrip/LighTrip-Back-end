package com.kauniv.lightrip.domain.scrap.repository;

import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    @Modifying
    @Query("DELETE FROM Scrap s WHERE s.passport.id = :passportId")
    void deleteAllByPassportId(Long passportId);

    boolean existsByUser_IdAndPassport_Id(Long userId, Long passportId);

    Optional<Scrap> findByUser_IdAndPassport_Id(Long userId, Long passportId);

    @Query("""
            SELECT s FROM Scrap s
            JOIN FETCH s.passport p
            LEFT JOIN FETCH p.images
            WHERE s.user.id = :userId
            ORDER BY s.id DESC
            """)
    List<Scrap> findByUserIdFirst(Long userId, Pageable pageable);

    @Query("""
            SELECT s FROM Scrap s
            JOIN FETCH s.passport p
            LEFT JOIN FETCH p.images
            WHERE s.user.id = :userId AND s.id < :cursor
            ORDER BY s.id DESC
            """)
    List<Scrap> findByUserIdAfterCursor(Long userId, Long cursor, Pageable pageable);

    long countByUser_Id(Long userId);

    @Query("""
            SELECT s.passport.id FROM Scrap s
            WHERE s.user.id = :userId AND s.passport.id IN :passportIds
            """)
    List<Long> findScrappedPassportIds(@Param("userId") Long userId,
                                       @Param("passportIds") List<Long> passportIds);

    // 내가 스크랩한 여권의 작성자 userId 목록 조회 (본인 제외, 중복 제거)
    @Query("""
    SELECT DISTINCT s.passport.user.id
    FROM Scrap s
    WHERE s.user.id = :userId
      AND s.passport.user.id <> :userId
    """)
    List<Long> findScrapedPassportOwnerIds(@Param("userId") Long userId);
}