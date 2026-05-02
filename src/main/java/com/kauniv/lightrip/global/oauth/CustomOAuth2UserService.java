package com.kauniv.lightrip.global.oauth;

import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.domain.user.service.UserService;
import com.kauniv.lightrip.global.oauth.userinfo.KakaoOAuth2UserInfo;
import com.kauniv.lightrip.global.oauth.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo;

        if (registrationId.equals("kakao")) {
            oAuth2UserInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        boolean isNewUser = !userService.existsBySocialInfo(oAuth2UserInfo.getProviderId(), socialType);

        Auth auth = userService.findOrCreateUser(oAuth2UserInfo, socialType);

        return new CustomOAuth2User(auth, oAuth2UserInfo, oAuth2User.getAttributes(), isNewUser);
    }
}