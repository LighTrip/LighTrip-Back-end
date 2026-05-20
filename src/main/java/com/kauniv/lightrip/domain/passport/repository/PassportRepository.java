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
            OR ST_DWithin(
                p.location::geography,
                ST_SetSRID(ST_MakePoint(CAST(:longitude AS float), CAST(:latitude AS float)), 4326)::geography,
                CAST(:radius AS float) * 1000
            )
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
    // > ST_DWithin: 두 지점 간 거리가 지정 미터 이내인지 판별.
    // > ::geography 캐스팅으로 지구 곡률 반영한 정확한 거리 계산.
    // > :radius * 1000: km → m 변환.
    // > GiST 인덱스(idx_passport_location) 활용으로 기존 Haversine 대비 성능 개선.

    @Query("""
        SELECT DISTINCT p FROM Passport p
        LEFT JOIN FETCH p.images
        JOIN FETCH p.user
        WHERE p.id IN :ids
        """)
    List<Passport> findAllByIdsForFeed(List<Long> ids);

    @Query(value = """
        SELECT DISTINCT p.*
        FROM passport p
        WHERE p.user_id = :targetUserId
          AND ST_Within(
              p.location,
              ST_MakeEnvelope(CAST(:minLng AS float), CAST(:minLat AS float),
                              CAST(:maxLng AS float), CAST(:maxLat AS float), 4326)
          )
          AND p.visibility IN (:visibilities)
        """, nativeQuery = true)
    List<Passport> findLightsInBounds(
            @Param("targetUserId") Long targetUserId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("visibilities") List<String> visibilities
    );
    // > ST_Within: 포인트가 사각형 영역 안에 있는지 확인.
    // > ST_MakeEnvelope: bounding box 생성 (minLng, minLat, maxLng, maxLat).
    // > GiST 인덱스 활용으로 기존 BETWEEN 대비 성능 개선.
    // > visibilities는 List<String>으로 받아서 네이티브 쿼리 IN절에 전달.

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