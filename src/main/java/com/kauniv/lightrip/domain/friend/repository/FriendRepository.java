package com.kauniv.lightrip.domain.friend.repository;

import com.kauniv.lightrip.domain.friend.entity.Friend;
import com.kauniv.lightrip.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("""
            SELECT COUNT(f) > 0 FROM Friend f
            WHERE f.status = 'ACCEPTED'
              AND ((f.requester.id = :userA AND f.receiver.id = :userB)
                OR (f.requester.id = :userB AND f.receiver.id = :userA))
            """)
    boolean isFriend(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
            SELECT COUNT(f) > 0 FROM Friend f
            WHERE (f.requester.id = :userA AND f.receiver.id = :userB)
               OR (f.requester.id = :userB AND f.receiver.id = :userA)
            """)
    boolean existsFriendship(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
            SELECT f FROM Friend f
            WHERE f.status = 'ACCEPTED'
              AND (f.requester.id = :userId OR f.receiver.id = :userId)
            """)
    List<Friend> findAllFriends(@Param("userId") Long userId);

    @Query("""
            SELECT f FROM Friend f
            WHERE f.status = 'PENDING'
              AND f.receiver.id = :userId
            """)
    List<Friend> findPendingRequests(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN f.requester.id = :userId THEN f.receiver.id ELSE f.requester.id END
            FROM Friend f
            WHERE f.status = 'ACCEPTED'
              AND ((f.requester.id = :userId AND f.receiver.id IN :otherIds)
                OR (f.receiver.id = :userId AND f.requester.id IN :otherIds))
            """)
    List<Long> findFriendUserIdsAmong(@Param("userId") Long userId,
                                      @Param("otherIds") List<Long> otherIds);

    // 두 유저의 공통 친구 목록 조회
    @Query("""
            SELECT CASE WHEN f1.requester.id = :userA THEN f1.receiver ELSE f1.requester END
            FROM Friend f1
            WHERE f1.status = 'ACCEPTED'
              AND (f1.requester.id = :userA OR f1.receiver.id = :userA)
              AND (
                CASE WHEN f1.requester.id = :userA THEN f1.receiver.id ELSE f1.requester.id END
              ) IN (
                SELECT CASE WHEN f2.requester.id = :userB THEN f2.receiver.id ELSE f2.requester.id END
                FROM Friend f2
                WHERE f2.status = 'ACCEPTED'
                  AND (f2.requester.id = :userB OR f2.receiver.id = :userB)
              )
            """)
    List<User> findMutualFriends(@Param("userA") Long userA, @Param("userB") Long userB);
}