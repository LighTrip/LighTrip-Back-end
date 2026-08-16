package com.kauniv.lightrip.global.oauth.userinfo;

import java.util.UUID;

// > Apple identity token(JWT)에는 nickname이 없음 — Apple은 최초 로그인 1회에 한해 fullName을
// > 클라이언트(RN)에만 전달하고, 백엔드로는 넘어오지 않음. 클라이언트가 그 값을 nickname으로
// > 함께 보내주면 사용하고, 없으면 임시 닉네임을 생성해서 User.nickname(NOT NULL, UNIQUE) 제약을 만족시킴.
// > 카카오와 동일하게 신규 유저는 온보딩(PATCH /api/v1/users/me/onboarding)에서 실제 닉네임을 다시 설정하므로
// > 여기서 만든 임시값은 그때까지의 자리표시자 역할만 함.
public class AppleOAuth2UserInfo implements OAuth2UserInfo {

    private final String providerId;
    private final String email;
    private final String nickname;

    public AppleOAuth2UserInfo(String providerId, String email, String nickname) {
        this.providerId = providerId;
        this.email = email;
        this.nickname = (nickname != null && !nickname.isBlank())
                ? nickname
                : "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getNickname() {
        return nickname;
    }
}
