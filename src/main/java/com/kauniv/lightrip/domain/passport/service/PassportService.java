package com.kauniv.lightrip.domain.passport.service;

import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.passport.dto.request.PassportCreateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportUpdateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportVisibilityRequest;
import com.kauniv.lightrip.domain.passport.dto.response.PassportResponse;
import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.entity.PassportImage;
import com.kauniv.lightrip.domain.passport.repository.DistrictCoverRepository;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.scrap.repository.ScrapRepository;
import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.team.repository.TeamMemberRepository;
import com.kauniv.lightrip.domain.team.repository.TeamRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.enums.District;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kauniv.lightrip.domain.passport.dto.response.DistrictResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kauniv.lightrip.domain.passport.dto.response.PassportListResponse;
import com.kauniv.lightrip.global.common.response.CursorResponse;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.domain.passport.dto.response.PassportStatsResponse;
import com.kauniv.lightrip.domain.like.repository.LikeRepository;
import com.kauniv.lightrip.domain.passport.dto.response.FeedPassportResponse;
import com.kauniv.lightrip.global.ai.service.AiService;
import com.kauniv.lightrip.global.common.response.FeedCursorResponse;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import com.kauniv.lightrip.domain.passport.dto.response.LightResponse;
import com.kauniv.lightrip.global.enums.Visibility;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassportService {

    private final PassportRepository passportRepository;
    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final FriendRepository friendRepository;
    private final DistrictCoverRepository districtCoverRepository;
    private final LikeRepository likeRepository;
    private final AiService aiService;
    // > 여권 저장 후 content 임베딩을 비동기로 저장하기 위해 주입.

    // ========== 등록 ==========
    @Transactional
    public PassportResponse create(Long userId, PassportCreateRequest req, String authorization) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (passportRepository.existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
                userId, req.latitude(), req.longitude(), req.visitedAt())) {
            throw new BusinessException(ErrorCode.PASSPORT_DUPLICATE);
        }

        Team team = null;
        if (req.teamId() != null) {
            team = teamRepository.findById(req.teamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            if (!teamMemberRepository.existsByTeam_IdAndUser_Id(team.getId(), userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        }

        Passport passport = Passport.builder()
                .user(user)
                .team(team)
                .content(req.content())
                .draft(req.draft())
                .aiCategory(req.aiCategory())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .address(req.address())
                .visitedAt(req.visitedAt())
                .district(req.district())
                .spaceName(req.spaceName())
                .category(req.category())
                .districtCategory(req.districtCategory())
                .visibility(req.visibilityOrDefault())
                .musicTitle(req.musicTitle())
                .musicArtist(req.musicArtist())
                .build();

        passportRepository.save(passport);
        passport.replaceImages(req.imageUrls());
        passportRepository.flush();

        createDistrictCoverIfFirst(user, passport);

        aiService.saveEmbeddingAsync(passport.getId(), req.content(), authorization);
        // > 여권 저장 완료 후 content를 비동기로 임베딩 → pgvector 저장.
        // > authorization: FastAPI 호출 시 JWT 검증에 필요. HTTP 요청에서 받아서 async 메서드에 전달.
        // > 응답 지연 없음. 실패해도 여권 저장에 영향 없음.

        return PassportResponse.from(passport);
    }

    // ========== 상세 조회 ==========
    public PassportResponse getPassport(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        validateReadPermission(passport, userId);
        Long coverId = districtCoverRepository
                .findByUser_IdAndDistrictCategory(passport.getUser().getId(), passport.getDistrictCategory())
                .map(DistrictCover::getId)
                .orElse(null);
        return PassportResponse.from(passport, coverId);
    }

    // ========== 수정 ==========
    @Transactional
    public PassportResponse update(Long userId, Long passportId, PassportUpdateRequest req) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        validateUpdatePermission(passport, userId);
        passport.update(req.content(), req.spaceName(), req.category(),
                req.districtCategory(), req.visibility(), req.musicTitle(), req.musicArtist());
        passport.replaceImages(req.imageUrls());
        return PassportResponse.from(passport);
    }

    // ========== 공개 범위 단독 변경 ==========
    @Transactional
    public PassportResponse updateVisibility(Long userId, Long passportId,
                                             PassportVisibilityRequest req) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        if (!passport.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }
        passport.updateVisibility(req.visibility());
        return PassportResponse.from(passport);
    }

    // ========== 삭제 ==========
    @Transactional
    public void delete(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        if (!passport.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }
        District district = passport.getDistrictCategory();
        likeRepository.deleteAllByPassportId(passportId);
        scrapRepository.deleteAllByPassportId(passportId);
        passportRepository.delete(passport);
        passportRepository.flush();
        cleanupDistrictCover(userId, district);
    }

    // ========== 외부 Service용 공개 메서드 ==========
    public Passport getPassportWithReadCheck(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        validateReadPermission(passport, userId);
        return passport;
    }

    // ========== DistrictCover 자동 생성 ==========
    private void createDistrictCoverIfFirst(User user, Passport passport) {
        District district = passport.getDistrictCategory();
        if (!districtCoverRepository.existsByUser_IdAndDistrictCategory(user.getId(), district)) {
            String firstImageUrl = passport.getImages().get(0).getImageUrl();
            DistrictCover cover = DistrictCover.builder()
                    .user(user)
                    .imageUrl(firstImageUrl)
                    .districtCategory(district)
                    .build();
            districtCoverRepository.save(cover);
        }
    }

    // ========== DistrictCover 정리 (삭제 시) ==========
    private void cleanupDistrictCover(Long userId, District district) {
        districtCoverRepository.findByUser_IdAndDistrictCategory(userId, district)
                .ifPresent(cover -> {
                    passportRepository.findFirstByUser_IdAndDistrictCategoryOrderByCreatedAtDesc(userId, district)
                            .ifPresentOrElse(
                                    latestPassport -> {
                                        if (!latestPassport.getImages().isEmpty()) {
                                            cover.changeCoverImage(latestPassport.getImages().get(0).getImageUrl());
                                        }
                                    },
                                    () -> districtCoverRepository.delete(cover)
                            );
                });
    }

    // ========== 수정 권한 검증 ==========
    private void validateUpdatePermission(Passport passport, Long userId) {
        if (passport.isTeamPassport()) {
            if (!teamMemberRepository.existsByTeam_IdAndUser_Id(passport.getTeam().getId(), userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        } else {
            if (!passport.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        }
    }

    // ========== 조회 권한 검증 ==========
    private void validateReadPermission(Passport passport, Long userId) {
        if (passport.isOwnedBy(userId)) return;
        if (passport.isTeamPassport()) {
            if (teamMemberRepository.existsByTeam_IdAndUser_Id(passport.getTeam().getId(), userId)) return;
        }
        switch (passport.getVisibility()) {
            case PUBLIC -> {}
            case FRIENDS_ONLY -> {
                if (!friendRepository.isFriend(userId, passport.getUser().getId())) {
                    throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
                }
            }
            case PRIVATE -> throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }
    }

    // ========== 내 기록 지역 조회 ==========
    public List<DistrictResponse> getMyDistricts(Long userId) {
        List<Object[]> counts = passportRepository.countByUserIdGroupByDistrict(userId);
        Map<District, DistrictCover> coverMap = districtCoverRepository.findAllByUser_Id(userId).stream()
                .collect(Collectors.toMap(DistrictCover::getDistrictCategory, c -> c));
        return counts.stream()
                .map(row -> {
                    District district = (District) row[0];
                    DistrictCover cover = coverMap.get(district);
                    String thumbnailUrl = cover != null ? cover.getImageUrl() : null;
                    String textColor = cover != null ? cover.getTextColor() : null;
                    Long coverId = cover != null ? cover.getId() : null;
                    return DistrictResponse.of(district, (Long) row[1], thumbnailUrl, textColor, coverId);
                })
                .toList();
    }

    // ========== 내 여권 목록 조회 ==========
    public CursorResponse<PassportListResponse> getMyPassports(Long userId, Category category,
                                                               District districtCategory,
                                                               Long cursor, int size) {
        List<Passport> passports = (cursor == null)
                ? passportRepository.findMyPassportsFirst(userId, category, districtCategory, PageRequest.of(0, size + 1))
                : passportRepository.findMyPassportsAfterCursor(userId, cursor, category, districtCategory, PageRequest.of(0, size + 1));
        boolean hasNext = passports.size() > size;
        if (hasNext) passports = passports.subList(0, size);
        Long nextCursor = hasNext ? passports.get(passports.size() - 1).getId() : null;
        return CursorResponse.of(passports.stream().map(PassportListResponse::from).toList(), hasNext, nextCursor);
    }

    // ========== 지역별 내 여권 상세 목록 조회 ==========
    public CursorResponse<PassportResponse> getMyPassportDetailsByDistrict(Long userId,
                                                                          District districtCategory,
                                                                          Long cursor, int size) {
        List<Passport> passports = (cursor == null)
                ? passportRepository.findMyPassportsFirst(userId, null, districtCategory, PageRequest.of(0, size + 1))
                : passportRepository.findMyPassportsAfterCursor(userId, cursor, null, districtCategory, PageRequest.of(0, size + 1));
        boolean hasNext = passports.size() > size;
        if (hasNext) passports = passports.subList(0, size);
        Long nextCursor = hasNext ? passports.get(passports.size() - 1).getId() : null;
        return CursorResponse.of(passports.stream().map(PassportResponse::from).toList(), hasNext, nextCursor);
    }

    // ========== 내 여권 통계 조회 ==========
    public PassportStatsResponse getMyStats(Long userId) {
        return new PassportStatsResponse(
                passportRepository.countByUser_Id(userId),
                likeRepository.countByUser_Id(userId),
                scrapRepository.countByUser_Id(userId)
        );
    }

    private static final int EARTH_RADIUS_KM = 6371;

    // ========== 릴스형 여권 피드 조회 ==========
    public FeedCursorResponse<FeedPassportResponse> getFeed(
            Long userId, Category category, District district,
            BigDecimal latitude, BigDecimal longitude,
            int radius, Long cursor, Long cursorScore, int size) {

        validateLocation(latitude, longitude);

        List<Long> candidateIds = passportRepository.findFeedPassportIds(
                userId, category != null ? category.name() : null,
                district != null ? district.name() : null,
                latitude, longitude, radius, cursor, cursorScore, size + 1
        );

        if (candidateIds.isEmpty()) return FeedCursorResponse.of(List.of(), false, null, null);

        boolean hasNext = candidateIds.size() > size;
        if (hasNext) candidateIds = candidateIds.subList(0, size);

        List<Passport> passports = passportRepository.findAllByIdsForFeed(candidateIds);
        Map<Long, Passport> passportMap = passports.stream().collect(Collectors.toMap(Passport::getId, p -> p));
        List<Passport> ordered = candidateIds.stream().map(passportMap::get).filter(Objects::nonNull).toList();

        Set<Long> likedIds = new HashSet<>(likeRepository.findLikedPassportIds(userId, candidateIds));
        Set<Long> scrappedIds = new HashSet<>(scrapRepository.findScrappedPassportIds(userId, candidateIds));
        List<Long> writerIds = ordered.stream().map(p -> p.getUser().getId()).distinct().toList();
        Set<Long> friendIds = new HashSet<>(friendRepository.findFriendUserIdsAmong(userId, writerIds));

        List<FeedPassportResponse> content = ordered.stream()
                .map(p -> {
                    BigDecimal distance = (latitude != null && longitude != null)
                            ? calculateDistance(latitude, longitude, p.getLatitude(), p.getLongitude()) : null;
                    return FeedPassportResponse.of(p, likedIds, scrappedIds, friendIds, distance);
                })
                .toList();

        Long nextCursor = null;
        Long nextCursorScore = null;
        if (hasNext && !content.isEmpty()) {
            FeedPassportResponse last = content.get(content.size() - 1);
            nextCursor = last.passportId();
            nextCursorScore = last.popularityScore();
        }

        return FeedCursorResponse.of(content, hasNext, nextCursor, nextCursorScore);
    }

    private void validateLocation(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                         BigDecimal lat2, BigDecimal lng2) {
        // > Haversine 공식은 피드 응답에서 각 여권까지의 거리 표시용으로 유지.
        // > DB 필터링은 PostGIS(ST_DWithin)가 하고, 응답에 포함할 거리값 계산만 여기서 처리.
        double rLat1 = Math.toRadians(lat1.doubleValue());
        double rLat2 = Math.toRadians(lat2.doubleValue());
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_KM * c).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== Visibility → String 변환 ==========
    private List<String> toStringList(List<Visibility> visibilities) {
        return visibilities.stream().map(Enum::name).toList();
        // > 네이티브 쿼리에서 Visibility enum을 직접 못 받으므로 String으로 변환.
    }

    // ========== 내 불빛 조회 ==========
    public List<LightResponse> getMyLights(Long userId, BigDecimal minLat, BigDecimal maxLat,
                                           BigDecimal minLng, BigDecimal maxLng, Long teamId) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);
        double cellSize = cellSizeForBBox(minLng, maxLng);

        // > teamId가 있으면 팀 모드: 팀 멤버십 검증 후 팀 여권만 클러스터링. visibility 무시.
        if (teamId != null) {
            if (!teamRepository.existsById(teamId)) {
                throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
            }
            if (!teamMemberRepository.existsByTeam_IdAndUser_Id(teamId, userId)) {
                throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
            }
            return mapClusterRows(passportRepository.findTeamLightClusters(
                    teamId, minLat, maxLat, minLng, maxLng, cellSize));
        }

        // > teamId 없으면 개인 모드: 본인 여권 전체 visibility 클러스터링.
        List<Visibility> allowed = List.of(
                Visibility.PUBLIC, Visibility.PRIVATE, Visibility.FRIENDS_ONLY
        );
        return mapClusterRows(passportRepository.findUserLightClusters(
                userId, minLat, maxLat, minLng, maxLng, toStringList(allowed), cellSize));
    }

    // ========== 특정 사용자 불빛 조회 ==========
    public List<LightResponse> getUserLights(Long viewerId, Long targetUserId,
                                             BigDecimal minLat, BigDecimal maxLat,
                                             BigDecimal minLng, BigDecimal maxLng) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);
        if (viewerId.equals(targetUserId)) {
            return getMyLights(viewerId, minLat, maxLat, minLng, maxLng, null);
        }
        if (!userRepository.existsById(targetUserId)) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        boolean isFriend = friendRepository.isFriend(viewerId, targetUserId);
        List<Visibility> allowed = isFriend
                ? List.of(Visibility.PUBLIC, Visibility.FRIENDS_ONLY)
                : List.of(Visibility.PUBLIC);

        double cellSize = cellSizeForBBox(minLng, maxLng);
        return mapClusterRows(passportRepository.findUserLightClusters(
                targetUserId, minLat, maxLat, minLng, maxLng, toStringList(allowed), cellSize));
    }

    // ========== BBox 폭 → 셀 크기(degree) 자동 계산 ==========
    // > 화면 폭의 1/50을 한 셀로 — 줌과 무관하게 항상 50칸 안팎의 격자로 클러스터링.
    // > 셀 내 1건이면 mapClusterRows에서 자동으로 단일 응답으로 떨어지므로 임계값 분기 불필요.
    private static double cellSizeForBBox(BigDecimal minLng, BigDecimal maxLng) {
        return maxLng.subtract(minLng).doubleValue() / 50.0;
    }

    // ========== 클러스터링 쿼리 결과 → LightResponse 매핑 ==========
    // > rows 각 행: [cnt(BIGINT), center_lat(numeric), center_lng(numeric), single_id(BIGINT or null)]
    // > cnt=1인 셀은 single_id로 Passport 일괄 fetch 후 from(p)로 단일 응답.
    // > cnt>=2인 셀은 cluster()로 묶음 응답 (단일 식별 필드 null).
    private List<LightResponse> mapClusterRows(List<Object[]> rows) {
        List<Long> singleIds = rows.stream()
                .map(r -> r[3] == null ? null : ((Number) r[3]).longValue())
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Passport> singleMap = singleIds.isEmpty()
                ? Map.of()
                : passportRepository.findAllByIdsForFeed(singleIds).stream()
                    .collect(Collectors.toMap(Passport::getId, p -> p));

        return rows.stream().map(r -> {
            long cnt = ((Number) r[0]).longValue();
            Long singleId = r[3] == null ? null : ((Number) r[3]).longValue();
            if (cnt == 1 && singleId != null) {
                Passport p = singleMap.get(singleId);
                if (p != null) return LightResponse.from(p);
            }
            BigDecimal centerLat = toBigDecimal(r[1]);
            BigDecimal centerLng = toBigDecimal(r[2]);
            return LightResponse.cluster(cnt, centerLat, centerLng);
        }).toList();
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return null;
    }

    // ========== 클러스터 셀 상세 리스트 ==========
    public CursorResponse<PassportListResponse> getLightCluster(
            Long userId, BigDecimal minLat, BigDecimal maxLat,
            BigDecimal minLng, BigDecimal maxLng, Long teamId,
            Long cursor, int size) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);

        List<Passport> passports;
        if (teamId != null) {
            if (!teamRepository.existsById(teamId)) {
                throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
            }
            if (!teamMemberRepository.existsByTeam_IdAndUser_Id(teamId, userId)) {
                throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
            }
            passports = passportRepository.findTeamPassportsInBounds(
                    teamId, minLat, maxLat, minLng, maxLng, cursor, PageRequest.of(0, size + 1));
        } else {
            List<Visibility> allowed = List.of(
                    Visibility.PUBLIC, Visibility.PRIVATE, Visibility.FRIENDS_ONLY
            );
            passports = passportRepository.findUserPassportsInBounds(
                    userId, allowed, minLat, maxLat, minLng, maxLng, cursor, PageRequest.of(0, size + 1));
        }

        boolean hasNext = passports.size() > size;
        if (hasNext) passports = passports.subList(0, size);
        Long nextCursor = hasNext ? passports.get(passports.size() - 1).getId() : null;
        return CursorResponse.of(
                passports.stream().map(PassportListResponse::from).toList(),
                hasNext, nextCursor
        );
    }

    private void validateBoundingBox(BigDecimal minLat, BigDecimal maxLat,
                                     BigDecimal minLng, BigDecimal maxLng) {
        if (minLat.compareTo(maxLat) > 0 || minLng.compareTo(maxLng) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}