package com.kauniv.lightrip.domain.user.service;

import com.kauniv.lightrip.domain.user.entity.Auth;
import com.kauniv.lightrip.domain.user.entity.CurrentMode;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.AuthRepository;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
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
}