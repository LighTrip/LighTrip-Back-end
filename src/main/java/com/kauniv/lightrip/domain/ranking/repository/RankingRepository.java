package com.kauniv.lightrip.domain.ranking.repository;

import com.kauniv.lightrip.domain.ranking.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    /**
     * 전체 랭킹 TOP 10 조회 (district IS NULL = 전체 랭킹)
     */
    List<Ranking> findTop10ByDistrictIsNullOrderByRankingPositionAsc();

    /**
     * 특정 유저의 전체 랭킹 순위 조회
     */
    Optional<Ranking> findByUser_IdAndDistrictIsNull(Long userId);

    /**
     * 주간 점수 집계 Native Query
     * 지난 7일간 받은 좋아요(×2) + 스크랩(×3)을 유저별로 합산
     */
    @Query(nativeQuery = true, value = """
            SELECT
                p.user_id AS userId,
                COALESCE(SUM(l.like_score), 0) + COALESCE(SUM(s.scrap_score), 0) AS totalScore
            FROM passport p
            LEFT JOIN (
                SELECT passport_id, COUNT(*) * 2 AS like_score
                FROM likes
                WHERE created_at >= NOW() - INTERVAL '7 days'
                GROUP BY passport_id
            ) l ON l.passport_id = p.passport_id
            LEFT JOIN (
                SELECT passport_id, COUNT(*) * 3 AS scrap_score
                FROM scrap
                WHERE created_at >= NOW() - INTERVAL '7 days'
                GROUP BY passport_id
            ) s ON s.passport_id = p.passport_id
            GROUP BY p.user_id
            HAVING COALESCE(SUM(l.like_score), 0) + COALESCE(SUM(s.scrap_score), 0) > 0
            ORDER BY totalScore DESC, p.user_id ASC
            """)
    List<Object[]> calculateWeeklyScores();
}