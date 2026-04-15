package com.kauniv.lightrip.domain.passport.entity;

import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "passport",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_passport_user_location_date",
                        columnNames = {"user_id", "latitude", "longitude", "visited_at"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Passport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passport_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "address", length = 50, nullable = false)
    private String address;

    @Column(name = "visited_at", nullable = false)
    private LocalDate visitedAt;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "space_name", length = 50)
    private String spaceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "district_category", nullable = false)
    private District districtCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility;

    @Column(name = "music_title", length = 100)
    private String musicTitle;

    @Column(name = "music_artist", length = 100)
    private String musicArtist;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public void update(String imageUrl,
                       String content,
                       String spaceName,
                       Category category,
                       District districtCategory,
                       Visibility visibility,
                       String musicTitle,
                       String musicArtist) {
        this.imageUrl = imageUrl;
        this.content = content;
        this.spaceName = spaceName;
        this.category = category;
        this.districtCategory = districtCategory;
        this.visibility = visibility;
        this.musicTitle = musicTitle;
        this.musicArtist = musicArtist;
    }

    /**
     * 해당 사용자가 이 여권의 작성자인지 확인
     */
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    /**
     * 팀 모드 여권 여부
     */
    public boolean isTeamPassport() {
        return this.team != null;
    }
}