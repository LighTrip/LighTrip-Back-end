package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.global.enums.District;
import java.util.Optional;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.Visibility;
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
        SELECT
            COUNT(*) AS cnt,
            AVG(p.latitude) AS center_lat,
            AVG(p.longitude) AS center_lng,
            CASE WHEN COUNT(*) = 1 THEN MIN(p.passport_id) END AS single_id
        FROM passport p
        WHERE p.user_id = :targetUserId
          AND ST_Within(
              p.location,
              ST_MakeEnvelope(CAST(:minLng AS float), CAST(:minLat AS float),
                              CAST(:maxLng AS float), CAST(:maxLat AS float), 4326)
          )
          AND p.visibility IN (:visibilities)
        GROUP BY ST_SnapToGrid(p.location, CAST(:cellSize AS float), CAST(:cellSize AS float))
        """, nativeQuery = true)
    List<Object[]> findUserLightClusters(
            @Param("targetUserId") Long targetUserId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("visibilities") List<String> visibilities,
            @Param("cellSize") double cellSize
    );
    // > ST_SnapToGrid로 location을 cellSize 격자에 스냅 → GROUP BY로 같은 셀끼리 묶음.
    // > 셀 내 1건이면 single_id에 passport_id 보존 (서비스에서 단일 응답으로 매핑).
    // > 반환: [cnt(BIGINT), center_lat(numeric), center_lng(numeric), single_id(BIGINT or null)]

    @Query(value = """
        SELECT
            COUNT(*) AS cnt,
            AVG(p.latitude) AS center_lat,
            AVG(p.longitude) AS center_lng,
            CASE WHEN COUNT(*) = 1 THEN MIN(p.passport_id) END AS single_id
        FROM passport p
        WHERE p.team_id = :teamId
          AND ST_Within(
              p.location,
              ST_MakeEnvelope(CAST(:minLng AS float), CAST(:minLat AS float),
                              CAST(:maxLng AS float), CAST(:maxLat AS float), 4326)
          )
        GROUP BY ST_SnapToGrid(p.location, CAST(:cellSize AS float), CAST(:cellSize AS float))
        """, nativeQuery = true)
    List<Object[]> findTeamLightClusters(
            @Param("teamId") Long teamId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("cellSize") double cellSize
    );
    // > 팀 여권용 클러스터링 (visibility 무시).

    @Query("""
        SELECT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.user.id = :userId
          AND p.visibility IN :visibilities
          AND p.latitude BETWEEN :minLat AND :maxLat
          AND p.longitude BETWEEN :minLng AND :maxLng
          AND (:cursor IS NULL OR p.id < :cursor)
        ORDER BY p.visitedAt DESC, p.id DESC
        """)
    List<Passport> findUserPassportsInBounds(
            @Param("userId") Long userId,
            @Param("visibilities") List<Visibility> visibilities,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
    // > 클러스터 셀 BBox 안의 여권을 visibility 필터링 후 커서 페이징.

    @Query("""
        SELECT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.team.id = :teamId
          AND p.latitude BETWEEN :minLat AND :maxLat
          AND p.longitude BETWEEN :minLng AND :maxLng
          AND (:cursor IS NULL OR p.id < :cursor)
        ORDER BY p.visitedAt DESC, p.id DESC
        """)
    List<Passport> findTeamPassportsInBounds(
            @Param("teamId") Long teamId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
    // > 팀 여권용 클러스터 셀 BBox 페이징 (visibility 무시).

    @Query("""
        SELECT p FROM Passport p
        LEFT JOIN FETCH p.images
        WHERE p.user.id = :userId
          AND p.visibility = com.kauniv.lightrip.global.enums.Visibility.PUBLIC
          AND (:district IS NULL OR p.districtCategory = :district)
        ORDER BY p.visitedAt DESC, p.id DESC
        """)
    List<Passport> findPublicPassportsByUserId(@Param("userId") Long userId,
                                               @Param("district") District district,
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

    @Modifying
    @Transactional
    @Query(value = "UPDATE passport SET embedding = CAST(:embedding AS vector) WHERE passport_id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);
    // > 여권 저장 후 비동기로 호출. EmbeddingGemma가 반환한 벡터를 pgvector 컬럼에 저장.
    // > CAST(:embedding AS vector): "[0.1,0.2,...]" 형식의 문자열을 vector 타입으로 변환.

    @Query(value = """
            SELECT content FROM passport
            WHERE user_id = :userId
              AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findSimilarContents(
            @Param("userId") Long userId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);
    // > RAG 초안 생성 시 코사인 유사도 기준 상위 기록 텍스트 반환.
    // > user_id 필터: 본인 기록만 검색 (타인 기록 유출 방지).
    // > embedding IS NOT NULL: 아직 임베딩이 채워지지 않은 기록 제외.
    // > <=>: pgvector 코사인 거리 연산자.
}