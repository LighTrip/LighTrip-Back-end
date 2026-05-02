package com.kauniv.lightrip.domain.friend.repository;

import com.kauniv.lightrip.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("""
            SELECT COUNT(f) > 0 FROM Friend f
            WHERE f.status = 'ACCEPTED'
              AND ((f.requester.id = :userA AND f.receiver.id = :userB)
                OR (f.requester.id = :userB AND f.receiver.id = :userA))
            """)
    boolean isFriend(Long userA, Long userB);
}