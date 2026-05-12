package com.kauniv.lightrip.domain.ranking.service;

import com.kauniv.lightrip.domain.ranking.dto.response.RankingResponse;
import com.kauniv.lightrip.domain.ranking.dto.response.TotalRankingResponse;
import com.kauniv.lightrip.domain.ranking.entity.Ranking;
import com.kauniv.lightrip.domain.ranking.repository.RankingRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final RankingRepository rankingRepository;
    private final UserRepository userRepository;

    /**
     * 전체 주간 랭킹 조회 (TOP 10 + 내 순위)
     */
    public TotalRankingResponse getTotalRanking(Long currentUserId) {
        // TOP 10 조회
        List<Ranking> top10 = rankingRepository.findTop10ByDistrictIsNullOrderByRankingPositionAsc();
        List<RankingResponse> topRankings = top10.stream()
                .map(RankingResponse::from)
                .toList();

        // 내가 TOP 10 안에 있는지 확인
        boolean isInTop10 = top10.stream()
                .anyMatch(r -> r.getUser().getId().equals(currentUserId));

        // TOP 10 밖이면 내 순위 별도 조회
        RankingResponse myRank = null;
        if (!isInTop10) {
            myRank = rankingRepository.findByUser_IdAndDistrictIsNull(currentUserId)
                    .map(RankingResponse::from)
                    .orElse(null);  // 점수 0점이면 null
        }

        return new TotalRankingResponse(topRankings, myRank);
    }

    /**
     * 주간 랭킹 산정 (스케줄러에서 호출)
     * 1. 기존 랭킹 전체 삭제
     * 2. 지난 7일간 점수 집계
     * 3. 순위 부여 후 저장
     */
    @Transactional
    public void calculateAndSaveRanking() {
        log.info("=== 주간 랭킹 산정 시작 ===");

        // 1. 기존 전체 삭제
        rankingRepository.deleteAllInBatch();

        // 2. 점수 집계
        List<Object[]> scores = rankingRepository.calculateWeeklyScores();
        log.info("점수 집계 완료: {}명", scores.size());

        if (scores.isEmpty()) {
            log.info("=== 이번 주 점수가 있는 유저 없음, 랭킹 산정 종료 ===");
            return;
        }

        // 3. 이번 주 월요일 00:00 계산
        LocalDateTime weekStartAt = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.MIDNIGHT);

        // 4. Ranking 엔티티 생성
        List<Ranking> rankings = new ArrayList<>();
        int rank = 1;

        for (Object[] row : scores) {
            Long userId = ((Number) row[0]).longValue();
            Integer totalScore = ((Number) row[1]).intValue();

            User user = userRepository.findById(userId)
                    .orElse(null);

            if (user == null) {
                log.warn("유저 {}를 찾을 수 없어 랭킹에서 제외", userId);
                continue;
            }

            rankings.add(Ranking.builder()
                    .user(user)
                    .score(totalScore)
                    .district(null)  // 전체 랭킹
                    .rankingPosition(rank++)
                    .weekStartAt(weekStartAt)
                    .build());
        }

        // 5. 일괄 저장
        rankingRepository.saveAll(rankings);
        log.info("=== 주간 랭킹 산정 완료: {}명 저장 ===", rankings.size());
    }
}