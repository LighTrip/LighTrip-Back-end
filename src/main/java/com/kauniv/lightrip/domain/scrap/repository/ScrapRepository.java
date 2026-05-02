package com.kauniv.lightrip.domain.scrap.repository;

import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    // 여권 삭제 시 연관 스크랩 일괄 삭제
    @Modifying
    @Query("DELETE FROM Scrap s WHERE s.passport.id = :passportId")
    void deleteAllByPassportId(Long passportId);

    // 중복 스크랩 체크
    boolean existsByUser_IdAndPassport_Id(Long userId, Long passportId);

    // 스크랩 취소용 조회
    Optional<Scrap> findByUser_IdAndPassport_Id(Long userId, Long passportId);

    // 커서 기반 목록 조회 (첫 요청)
    @Query("""
            SELECT s FROM Scrap s
            JOIN FETCH s.passport p
            LEFT JOIN FETCH p.images
            WHERE s.user.id = :userId
            ORDER BY s.id DESC
            LIMIT :size
            """)
    List<Scrap> findByUserIdFirst(Long userId, int size);

    // 커서 기반 목록 조회 (다음 페이지)
    @Query("""
            SELECT s FROM Scrap s
            JOIN FETCH s.passport p
            LEFT JOIN FETCH p.images
            WHERE s.user.id = :userId AND s.id < :cursor
            ORDER BY s.id DESC
            LIMIT :size
            """)
    List<Scrap> findByUserIdAfterCursor(Long userId, Long cursor, int size);
}