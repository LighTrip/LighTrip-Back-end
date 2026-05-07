package com.kauniv.lightrip.domain.user.service;

import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.like.repository.LikeRepository;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.scrap.repository.ScrapRepository;
import com.kauniv.lightrip.domain.user.dto.response.MyProfileResponse;
import com.kauniv.lightrip.domain.user.dto.response.PublicProfileResponse;
import com.kauniv.lightrip.domain.user.dto.request.UpdateProfileRequest;
import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.domain.user.entity.CurrentMode;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.global.auth.repository.AuthRepository;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.oauth.SocialType;
import com.kauniv.lightrip.global.oauth.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PassportRepository passportRepository;
    private final FriendRepository friendRepository;
    private final LikeRepository likeRepository;
    private final ScrapRepository scrapRepository;

    public boolean existsBySocialInfo(String socialId, SocialType socialType) {
        return authRepository.findBySocialIdAndSocialType(socialId, socialType).isPresent();
    }

    public Auth findOrCreateUser(OAuth2UserInfo oAuth2UserInfo, SocialType socialType) {
        return authRepository
                .findBySocialIdAndSocialType(oAuth2UserInfo.getProviderId(), socialType)
                .orElseGet(() -> createUser(oAuth2UserInfo, socialType));
    }

    private Auth createUser(OAuth2UserInfo oAuth2UserInfo, SocialType socialType) {
        User user = userRepository.save(
                User.builder()
                        .nickname(oAuth2UserInfo.getNickname())
                        .email(oAuth2UserInfo.getEmail())
                        .currentMode(CurrentMode.INDIVIDUAL)
                        .build()
        );

        return authRepository.save(
                Auth.builder()
                        .user(user)
                        .socialId(oAuth2UserInfo.getProviderId())
                        .socialType(socialType)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        MyProfileResponse.Stats stats = MyProfileResponse.Stats.builder()
                .passportCount(passportRepository.countByUser_Id(userId))
                .friendCount(friendRepository.findAllFriends(userId).size())
                .likeCount(likeRepository.countByUser_Id(userId))
                .scrapCount(scrapRepository.countByUser_Id(userId))
                .build();

        return MyProfileResponse.from(user, stats);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getNickname().equals(request.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateProfile(request.getNickname(), request.getProfileImg(), request.getLocation(), request.getBio());

        MyProfileResponse.Stats stats = MyProfileResponse.Stats.builder()
                .passportCount(passportRepository.countByUser_Id(userId))
                .friendCount(friendRepository.findAllFriends(userId).size())
                .likeCount(likeRepository.countByUser_Id(userId))
                .scrapCount(scrapRepository.countByUser_Id(userId))
                .build();

        return MyProfileResponse.from(user, stats);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return PublicProfileResponse.from(user);
    }
}