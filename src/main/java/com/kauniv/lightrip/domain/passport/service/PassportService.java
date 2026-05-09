package com.kauniv.lightrip.domain.passport.service;

import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.passport.dto.request.PassportCreateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportUpdateRequest;
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
        passportRepository.flush(); // 이미지 ID 생성을 위해 flush

        // DistrictCover 자동 생성 (해당 구에 첫 등록이면)
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

        passport.update(
                req.content(), req.spaceName(),
                req.category(), req.districtCategory(), req.visibility(),
                req.musicTitle(), req.musicArtist()
        );

        passport.replaceImages(req.imageUrls());

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
                    // 해당 구에 다른 여권이 남아있는지 확인
                    passportRepository.findFirstByUser_IdAndDistrictCategoryOrderByCreatedAtDesc(userId, district)
                            .ifPresentOrElse(
                                    // 남은 여권 있으면 → 그 여권의 첫 이미지로 교체
                                    latestPassport -> {
                                        if (!latestPassport.getImages().isEmpty()) {
                                            cover.changeCoverImage(latestPassport.getImages().get(0));
                                        }
                                    },
                                    // 남은 여권 없으면 → DistrictCover 삭제
                                    () -> districtCoverRepository.delete(cover)
                            );
                });
    }

    // ========== 수정 권한 검증 ==========
    private void validateUpdatePermission(Passport passport, Long userId) {
        if (passport.isTeamPassport()) {
            boolean isMember = teamMemberRepository.existsByTeam_IdAndUser_Id(
                    passport.getTeam().getId(), userId
            );
            if (!isMember) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        } else {
            if (!passport.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        }
    }

    // ========== 조회 권한 검증 (Visibility) ==========
    private void validateReadPermission(Passport passport, Long userId) {
        if (passport.isOwnedBy(userId)) {
            return;
        }

        if (passport.isTeamPassport()) {
            boolean isMember = teamMemberRepository.existsByTeam_IdAndUser_Id(
                    passport.getTeam().getId(), userId
            );
            if (isMember) {
                return;
            }
        }

        switch (passport.getVisibility()) {
            case PUBLIC -> {
                return;
            }
            case FRIENDS_ONLY -> {
                Long ownerId = passport.getUser().getId();
                if (friendRepository.isFriend(userId, ownerId)) {
                    return;
                }
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
            case PRIVATE -> throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }
    }

    // ========== 내 기록 지역 조회 ==========
    public List<DistrictResponse> getMyDistricts(Long userId) {
        // 1. 지역별 여권 수 조회
        List<Object[]> counts = passportRepository.countByUserIdGroupByDistrict(userId);

        // 2. 사용자의 DistrictCover 전체 조회 → Map으로 변환
        Map<District, String> coverMap = districtCoverRepository.findAllByUser_Id(userId).stream()
                .collect(Collectors.toMap(
                        DistrictCover::getDistrictCategory,
                        cover -> cover.getPassportImage().getImageUrl()
                ));

        // 3. 합치기
        return counts.stream()
                .map(row -> {
                    District district = (District) row[0];
                    Long count = (Long) row[1];
                    String thumbnailUrl = coverMap.get(district);
                    return DistrictResponse.of(district, count, thumbnailUrl);
                })
                .toList();
    }


    // ========== 내 여권 목록 조회 (장소별, 커서 기반) ==========
    public CursorResponse<PassportListResponse> getMyPassports(Long userId,
                                                               Category category,
                                                               District districtCategory,
                                                               Long cursor,
                                                               int size) {
        List<Passport> passports = (cursor == null)
                ? passportRepository.findMyPassportsFirst(userId, category, districtCategory, PageRequest.of(0, size + 1))
                : passportRepository.findMyPassportsAfterCursor(userId, cursor, category, districtCategory, PageRequest.of(0, size + 1));
        boolean hasNext = passports.size() > size;

        if (hasNext) {
            passports = passports.subList(0, size);
        }

        List<PassportListResponse> content = passports.stream()
                .map(PassportListResponse::from)
                .toList();

        Long nextCursor = hasNext ? passports.get(passports.size() - 1).getId() : null;

        return CursorResponse.of(content, hasNext, nextCursor);
    }

    // ========== 내 여권 통계 조회 ==========
    public PassportStatsResponse getMyStats(Long userId) {
        long passportCount = passportRepository.countByUser_Id(userId);
        long likeCount = likeRepository.countByUser_Id(userId);
        long scrapCount = scrapRepository.countByUser_Id(userId);

        return new PassportStatsResponse(passportCount, likeCount, scrapCount);
    }

    private static final int EARTH_RADIUS_KM = 6371;

    // ========== 릴스형 여권 피드 조회 ==========
    public FeedCursorResponse<FeedPassportResponse> getFeed(
            Long userId,
            Category category,
            District district,
            BigDecimal latitude,
            BigDecimal longitude,
            int radius,
            Long cursor,
            Long cursorScore,
            int size
    ) {
        // 1. 위치 파라미터 검증
        validateLocation(latitude, longitude);

        // 2. 후보 여권 ID 조회 (정렬된 상태)
        List<Long> candidateIds = passportRepository.findFeedPassportIds(
                userId,
                category != null ? category.name() : null,
                district != null ? district.name() : null,
                latitude,
                longitude,
                radius,
                cursor,
                cursorScore,
                size + 1
        );

        if (candidateIds.isEmpty()) {
            return FeedCursorResponse.of(List.of(), false, null, null);
        }

        boolean hasNext = candidateIds.size() > size;
        if (hasNext) {
            candidateIds = candidateIds.subList(0, size);
        }

        // 3. 여권 엔티티 fetch join (N+1 방지)
        List<Passport> passports = passportRepository.findAllByIdsForFeed(candidateIds);

        // 4. 정렬 순서 복원 (IN 쿼리는 순서 보장 X)
        Map<Long, Passport> passportMap = passports.stream()
                .collect(Collectors.toMap(Passport::getId, p -> p));
        List<Passport> ordered = candidateIds.stream()
                .map(passportMap::get)
                .filter(Objects::nonNull)
                .toList();

        // 5. 좋아요/스크랩/친구 일괄 조회
        Set<Long> likedIds = new HashSet<>(likeRepository.findLikedPassportIds(userId, candidateIds));
        Set<Long> scrappedIds = new HashSet<>(scrapRepository.findScrappedPassportIds(userId, candidateIds));

        List<Long> writerIds = ordered.stream()
                .map(p -> p.getUser().getId())
                .distinct()
                .toList();
        Set<Long> friendIds = new HashSet<>(friendRepository.findFriendUserIdsAmong(userId, writerIds));

        // 6. 응답 구성
        List<FeedPassportResponse> content = ordered.stream()
                .map(p -> {
                    BigDecimal distance = (latitude != null && longitude != null)
                            ? calculateDistance(latitude, longitude, p.getLatitude(), p.getLongitude())
                            : null;
                    return FeedPassportResponse.of(p, likedIds, scrappedIds, friendIds, distance);
                })
                .toList();

        // 7. nextCursor 추출
        Long nextCursor = null;
        Long nextCursorScore = null;
        if (hasNext && !content.isEmpty()) {
            FeedPassportResponse last = content.get(content.size() - 1);
            nextCursor = last.passportId();
            nextCursorScore = last.popularityScore();
        }

        return FeedCursorResponse.of(content, hasNext, nextCursor, nextCursorScore);
    }

    // ========== 위치 파라미터 검증 ==========
    private void validateLocation(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    // ========== Haversine 거리 계산 ==========
    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                         BigDecimal lat2, BigDecimal lng2) {
        double rLat1 = Math.toRadians(lat1.doubleValue());
        double rLat2 = Math.toRadians(lat2.doubleValue());
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== 내 불빛 조회 ==========
    public List<LightResponse> getMyLights(Long userId,
                                           BigDecimal minLat, BigDecimal maxLat,
                                           BigDecimal minLng, BigDecimal maxLng) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);

        // 본인은 모든 visibility 노출
        List<Visibility> allowed = List.of(
                Visibility.PUBLIC,
                Visibility.PRIVATE,
                Visibility.FRIENDS_ONLY
        );

        return passportRepository.findLightsInBounds(
                        userId, minLat, maxLat, minLng, maxLng, allowed)
                .stream()
                .map(LightResponse::from)
                .toList();
    }

    // ========== 특정 사용자 불빛 조회 ==========
    public List<LightResponse> getUserLights(Long viewerId, Long targetUserId,
                                             BigDecimal minLat, BigDecimal maxLat,
                                             BigDecimal minLng, BigDecimal maxLng) {
        validateBoundingBox(minLat, maxLat, minLng, maxLng);

        // 본인 조회면 me 메서드로 위임
        if (viewerId.equals(targetUserId)) {
            return getMyLights(viewerId, minLat, maxLat, minLng, maxLng);
        }

        // 대상 사용자 존재 검증
        if (!userRepository.existsById(targetUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 친구 여부에 따라 visibility 필터링
        boolean isFriend = friendRepository.isFriend(viewerId, targetUserId);
        List<Visibility> allowed = isFriend
                ? List.of(Visibility.PUBLIC, Visibility.FRIENDS_ONLY)
                : List.of(Visibility.PUBLIC);

        return passportRepository.findLightsInBounds(
                        targetUserId, minLat, maxLat, minLng, maxLng, allowed)
                .stream()
                .map(LightResponse::from)
                .toList();
    }

    // ========== Bounding Box 검증 ==========
    private void validateBoundingBox(BigDecimal minLat, BigDecimal maxLat,
                                     BigDecimal minLng, BigDecimal maxLng) {
        if (minLat.compareTo(maxLat) > 0 || minLng.compareTo(maxLng) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

}