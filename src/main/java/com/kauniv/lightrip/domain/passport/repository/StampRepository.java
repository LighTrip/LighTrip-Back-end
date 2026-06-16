package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.domain.passport.entity.Stamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StampRepository extends JpaRepository<Stamp, Long> {

    @Query("""
    SELECT s.passport.user.id, COUNT(s)
    FROM Stamp s
    WHERE s.passport.user.id IN :userIds
    GROUP BY s.passport.user.id
    """)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);
}
