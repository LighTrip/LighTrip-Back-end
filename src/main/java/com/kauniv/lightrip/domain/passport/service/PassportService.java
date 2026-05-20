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

    // ========== 등록 ==========
    @Transactional
    public PassportResponse create(Long userId, PassportCreateRequest req) {
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

        return PassportResponse.from(passport);
    }

    // ========== 상세 조회 ==========
    public PassportResponse getPassport(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        validateReadPermission(passport, userId);
        return PassportResponse.from(passport);
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
            PassportImage firstImage = passport.getImages().get(0);
            DistrictCover cover = DistrictCover.builder()
                    .user(user)
                    .passportImage(firstImage)
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
                                            cover.changeCoverImage(latestPassport.getImages().get(0));
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
        Map<District, String> coverMap = districtCoverRepository.findAllByUser_Id(userId).stream()
                .collect(Collectors.toMap(
                        DistrictCover::getDistrictCategory,
                        cover -> cover.getPassportImage().getImageUrl()
                ));
        return counts.stream()
                .map(row -> DistrictResponse.of((District) row[0], (Long) row[1], coverMap.get((District) row[0])))
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
                                           BigDecimal minLng, BigDecimal maxLng) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);
        List<Visibility> allowed = List.of(
                Visibility.PUBLIC, Visibility.PRIVATE, Visibility.FRIENDS_ONLY
        );
        return passportRepository.findLightsInBounds(userId, minLat, maxLat, minLng, maxLng,
                        toStringList(allowed))
                .stream().map(LightResponse::from).toList();
    }

    // ========== 특정 사용자 불빛 조회 ==========
    public List<LightResponse> getUserLights(Long viewerId, Long targetUserId,
                                             BigDecimal minLat, BigDecimal maxLat,
                                             BigDecimal minLng, BigDecimal maxLng) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);
        if (viewerId.equals(targetUserId)) return getMyLights(viewerId, minLat, maxLat, minLng, maxLng);
        if (!userRepository.existsById(targetUserId)) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        boolean isFriend = friendRepository.isFriend(viewerId, targetUserId);
        List<Visibility> allowed = isFriend
                ? List.of(Visibility.PUBLIC, Visibility.FRIENDS_ONLY)
                : List.of(Visibility.PUBLIC);
        return passportRepository.findLightsInBounds(targetUserId, minLat, maxLat, minLng, maxLng,
                        toStringList(allowed))
                .stream().map(LightResponse::from).toList();
    }

    private void validateBoundingBox(BigDecimal minLat, BigDecimal maxLat,
                                     BigDecimal minLng, BigDecimal maxLng) {
        if (minLat.compareTo(maxLat) > 0 || minLng.compareTo(maxLng) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}