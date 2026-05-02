package com.kauniv.lightrip.global.oauth;

import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.oauth.userinfo.OAuth2UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final Auth auth;
    private final OAuth2UserInfo oAuth2UserInfo;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(Auth auth, OAuth2UserInfo oAuth2UserInfo, Map<String, Object> attributes) {
        this.auth = auth;
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return oAuth2UserInfo.getProviderId();
    }

    public Long getUserId() {
        return auth.getUser().getId();
    }

    public String getEmail() {
        return oAuth2UserInfo.getEmail();
    }

    public String getNickname() {
        return oAuth2UserInfo.getNickname();
    }

    public SocialType getSocialType() {
        return auth.getSocialType();
    }
}