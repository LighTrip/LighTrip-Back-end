package com.kauniv.lightrip.domain.block.repository;

import com.kauniv.lightrip.domain.block.entity.UserBlock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    Optional<UserBlock> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    // > 두 사용자가 어느 방향으로든 차단 관계인지. 친구 요청/단건 조회 등 1:1 판정에 사용.
    @Query("""
            SELECT COUNT(b) > 0 FROM UserBlock b
             WHERE (b.blocker.id = :userA AND b.blocked.id = :userB)
                OR (b.blocker.id = :userB AND b.blocked.id = :userA)
            """)
    boolean existsBlockBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    // > "내가 차단한 사람" ∪ "나를 차단한 사람" id 집합.
    // > 피드/추천 등 목록 조회에서 요청당 1회 조회 후 재사용 (N+1 방지).
    @Query("""
            SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END
              FROM UserBlock b
             WHERE b.blocker.id = :userId OR b.blocked.id = :userId
            """)
    List<Long> findRelatedUserIds(@Param("userId") Long userId);

    // > 내 차단 목록 화면용 (최신순, 커서 페이징). 상대 User를 fetch join.
    @Query("""
            SELECT b FROM UserBlock b
             JOIN FETCH b.blocked
             WHERE b.blocker.id = :blockerId
             ORDER BY b.id DESC
            """)
    List<UserBlock> findMyBlocksFirst(@Param("blockerId") Long blockerId, Pageable pageable);

    @Query("""
            SELECT b FROM UserBlock b
             JOIN FETCH b.blocked
             WHERE b.blocker.id = :blockerId
               AND b.id < :cursor
             ORDER BY b.id DESC
            """)
    List<UserBlock> findMyBlocksAfterCursor(@Param("blockerId") Long blockerId,
                                            @Param("cursor") Long cursor,
                                            Pageable pageable);
}
