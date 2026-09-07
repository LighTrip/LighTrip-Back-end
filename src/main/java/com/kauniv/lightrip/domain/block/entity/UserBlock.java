package com.kauniv.lightrip.domain.block.entity;

import com.kauniv.lightrip.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 차단 1건. {@code blocker} 가 {@code blocked} 를 차단한 단방향 레코드.
 *
 * <p>노출 필터링은 "내가 차단한 사람"과 "나를 차단한 사람"을 모두 숨겨야 하므로,
 * 조회 시 blocker/blocked 양방향으로 id 집합을 만든다
 * ({@link com.kauniv.lightrip.domain.block.repository.UserBlockRepository#findRelatedUserIds}).</p>
 */
@Entity
@Table(
        name = "user_block",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_block_blocker_blocked",
                        columnNames = {"blocker_id", "blocked_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_block_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
