// domain/passport/repository/PassportRepository.java
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

    // 중복 등록 방지 (uk_passport_user_location_date)
    boolean existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
            Long userId, BigDecimal latitude, BigDecimal longitude, LocalDate visitedAt
    );

    // 해당 구에 사용자 여권이 있는지 확인
    boolean existsByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    // 해당 구의 가장 최근 여권 조회 (삭제 시 대표 이미지 교체용)
    Optional<Passport> findFirstByUser_IdAndDistrictCategoryOrderByCreatedAtDesc(
            Long userId, District districtCategory
    );


    // 사용자의 지역별 여권 수 조회
    @Query("""
        SELECT p.districtCategory, COUNT(p)
        FROM Passport p
        WHERE p.user.id = :userId
        GROUP BY p.districtCategory
        ORDER BY COUNT(p) DESC
        """)
    List<Object[]> countByUserIdGroupByDistrict(Long userId);

    // 장소별 여권 목록 (필터 + 커서, 첫 요청)
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

    // 장소별 여권 목록 (필터 + 커서, 다음 페이지)
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

    // ========== 피드 조회: 인기순 + 필터 + 커서 (정렬된 ID 리스트) ==========
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

    // ========== ID 리스트로 여권 + 이미지 + 작성자 fetch ==========
    @Query("""
        SELECT DISTINCT p FROM Passport p
        LEFT JOIN FETCH p.images
        JOIN FETCH p.user
        WHERE p.id IN :ids
        """)
    List<Passport> findAllByIdsForFeed(List<Long> ids);
}