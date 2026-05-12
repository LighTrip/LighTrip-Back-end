package com.kauniv.lightrip.domain.ranking.entity;

import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.global.enums.District;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ranking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ranking_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score", nullable = false)
    private Integer score;  // 주간 가중치 합산 점수: (좋아요 × 2) + (스크랩 × 3)

    @Enumerated(EnumType.STRING)
    @Column(name = "district", length = 50)
    private District district;  // NULL이면 전체 랭킹, 값 있으면 구별 랭킹

    @Column(name = "ranking_position", nullable = false)
    private Integer rankingPosition;

    @Column(name = "week_start_at", nullable = false)
    private LocalDateTime weekStartAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}