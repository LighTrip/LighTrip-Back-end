package com.kauniv.lightrip.domain.friend.repository;

import com.kauniv.lightrip.domain.friend.entity.Friend;
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
}