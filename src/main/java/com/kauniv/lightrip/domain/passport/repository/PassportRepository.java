package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.global.enums.District;
import java.util.Optional;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import com.kauniv.lightrip.global.enums.Category;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.kauniv.lightrip.global.enums.Visibility;

public interface PassportRepository extends JpaRepository<Passport, Long> {

    boolean existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
            Long userId, BigDecimal latitude, BigDecimal longitude, LocalDate visitedAt
    );

    boolean existsByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    Optional<Passport> findFirstByUser_IdAndDistrictCategoryOrderByCreatedAtDesc(
            Long userId, District districtCategory
    );

    @Query("""
        SELECT p.districtCategory, COUNT(p)
        FROM Passport p
        WHERE p.user.id = :userId
        GROUP BY p.districtCategory
        ORDER BY COUNT(p) DESC
        """)
    List<Object[]> countByUserIdGroupByDistrict(Long userId);

    // 친구 지도 조회: PUBLIC 여권 기준 district별 여권 수
    @Query("""
        SELECT p.districtCategory, COUNT(p)
        FROM Passport p
        WHERE p.user.id = :userId
          AND p.visibility = com.kauniv.lightrip.global.enums.Visibility.PUBLIC
        GROUP BY p.districtCategory
        ORDER BY COUNT(p) DESC
        """)
    List<Object[]> countPublicByUserIdGroupByDistrict(@Param("userId") Long userId);

    @Query("""
        SELECT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.user.id = :userId
          AND (:category IS NULL OR p.category = :category)
          AND (:districtCategory IS NULL OR p.districtCategory = :districtCategory)
        ORDER BY p.visitedAt DESC, p.id DESC
        """)
    List<Passport> findMyPassportsFirst(Long userId,
                                        Category category,
                                        District districtCategory,
                                        Pageable pageable);

    @Query("""
        SELECT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.user.id = :userId
          AND p.id < :cursor
          AND (:category IS NULL OR p.category = :category)
          AND (:districtCategory IS NULL OR p.districtCategory = :districtCategory)
        ORDER BY p.visitedAt DESC, p.id DESC
        """)
    List<Passport> findMyPassportsAfterCursor(Long userId,
                                              Long cursor,
                                              Category category,
                                              District districtCategory,
                                              Pageable pageable);

    long countByUser_Id(Long userId);

    @Query(value = """
        SELECT p.passport_id
        FROM passport p
        WHERE p.user_id <> :userId
          AND p.visibility = 'PUBLIC'
          AND (CAST(:category AS varchar) IS NULL
               OR p.category = CAST(:category AS varchar))
          AND (CAST(:district AS varchar) IS NULL
               OR p.district_category = CAST(:district AS varchar))
          AND (
            :latitude IS NULL OR :longitude IS NULL
            OR (6371 * acos(
                  LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:latitude)) * cos(radians(p.latitude))
                    * cos(radians(p.longitude) - radians(:longitude))
                    + sin(radians(:latitude)) * sin(radians(p.latitude))
                  ))
                )) <= :radius
          )
          AND (
            :cursor IS NULL OR :cursorScore IS NULL
            OR (p.like_count * 2 + p.scrap_count * 3) < :cursorScore
            OR ((p.like_count * 2 + p.scrap_count * 3) = :cursorScore
                AND p.passport_id < :cursor)
          )
        ORDER BY (p.like_count * 2 + p.scrap_count * 3) DESC, p.passport_id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findFeedPassportIds(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("district") String district,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radius") int radius,
            @Param("cursor") Long cursor,
            @Param("cursorScore") Long cursorScore,
            @Param("limit") int limit
    );

    @Query("""
        SELECT DISTINCT p FROM Passport p
        LEFT JOIN FETCH p.images
        JOIN FETCH p.user
        WHERE p.id IN :ids
        """)
    List<Passport> findAllByIdsForFeed(List<Long> ids);

    @Query("""
        SELECT DISTINCT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.user.id = :targetUserId
          AND p.latitude BETWEEN :minLat AND :maxLat
          AND p.longitude BETWEEN :minLng AND :maxLng
          AND p.visibility IN :visibilities
        """)
    List<Passport> findLightsInBounds(
            Long targetUserId,
            BigDecimal minLat, BigDecimal maxLat,
            BigDecimal minLng, BigDecimal maxLng,
            List<Visibility> visibilities
    );

    @Query("""
        SELECT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.user.id = :userId
          AND p.visibility = com.kauniv.lightrip.global.enums.Visibility.PUBLIC
        ORDER BY p.visitedAt DESC, p.id DESC
        """)
    List<Passport> findPublicPassportsByUserId(@Param("userId") Long userId,
                                               Pageable pageable);

    List<Passport> findAllByTeam_Id(Long teamId);

    // 여러 유저의 도장 수를 한번에 조회 (N+1 방지)
    @Query("""
    SELECT p.user.id, COUNT(p)
    FROM Passport p
    WHERE p.user.id IN :userIds
    GROUP BY p.user.id
    """)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);

    @Query("""
    SELECT COUNT(DISTINCT p.districtCategory)
    FROM Passport p
    WHERE p.user.id = :userId
    """)
    long countDistinctDistrictByUserId(@Param("userId") Long userId);
}