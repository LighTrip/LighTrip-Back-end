package com.kauniv.lightrip.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "profile_img", columnDefinition = "TEXT")
    private String profileImg;

    @Column(name = "friend_code", nullable = false, unique = true, length = 8)
    private String friendCode;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "theme_color", nullable = false, length = 7)
    @Builder.Default
    private String themeColor = "#FFFFFF";
    // > 프로필 테마 색상 (HEX #RRGGBB). 기본 흰색.

    @Column(name = "passport_theme", columnDefinition = "TEXT")
    private String passportTheme;
    // > 여권 테마 (테마 이름 또는 이미지 URL). nullable.

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateFriendCode() {
        if (this.friendCode == null) {
            this.friendCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public void updateProfile(String nickname, String profileImg) {
        this.nickname = nickname;
        this.profileImg = profileImg;
    }

    public void updateProfile(String nickname, String profileImg, String location, String bio) {
        this.nickname = nickname;
        this.profileImg = profileImg;
        this.location = location;
        this.bio = bio;
    }

    public void changeThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public void changePassportTheme(String passportTheme) {
        this.passportTheme = passportTheme;
    }
}