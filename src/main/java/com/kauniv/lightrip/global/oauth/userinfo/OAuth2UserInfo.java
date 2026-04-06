package com.kauniv.lightrip.global.oauth.userinfo;

public interface OAuth2UserInfo {
        String getProviderId(); // OAuth2 제공자에서 고유한 사용자 ID
        String getEmail(); // 이메일
        String getNickname(); // 닉네임
}
