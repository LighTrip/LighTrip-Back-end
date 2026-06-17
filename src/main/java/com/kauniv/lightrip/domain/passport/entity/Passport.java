package com.kauniv.lightrip.domain.passport.entity;

import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")
    @Builder.Default
    private List<PassportImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Stamp> stamps = new ArrayList<>();

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "draft", columnDefinition = "TEXT")
    private String draft;
    // > AI가 생성한 블로그 초안 원본.
    // > 사용자가 content를 수정해도 초안은 보존 → 향후 학습 데이터셋으로 활용.

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_category", length = 20)
    private Category aiCategory;
    // > AI가 분류한 카테고리 초기값.
    // > 사용자가 category를 바꿔도 ai_category는 보존.
    // > category != ai_category 케이스 분석으로 모델 개선에 활용.

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

    @Column(name = "theme", length = 30)
    private String theme;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    @Column(name = "scrap_count", nullable = false)
    @Builder.Default
    private Long scrapCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String content, String spaceName, Category category,
                       District districtCategory, Visibility visibility,
                       String musicTitle, String musicArtist, String theme) {
        this.content = content;
        this.spaceName = spaceName;
        this.category = category;
        this.districtCategory = districtCategory;
        this.visibility = visibility;
        this.musicTitle = musicTitle;
        this.musicArtist = musicArtist;
        this.theme = theme;
    }

    public void replaceImages(List<String> imageUrls) {
        this.images.clear();
        for (int i = 0; i < imageUrls.size(); i++) {
            this.images.add(
                    PassportImage.builder()
                            .passport(this)
                            .imageUrl(imageUrls.get(i))
                            .imageOrder(i + 1)
                            .build()
            );
        }
    }

    public void updateVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isTeamPassport() {
        return this.team != null;
    }

    public void increaseLikeCount() { this.likeCount++; }
    public void decreaseLikeCount() { if (this.likeCount > 0) this.likeCount--; }
    public void increaseScrapCount() { this.scrapCount++; }
    public void decreaseScrapCount() { if (this.scrapCount > 0) this.scrapCount--; }
}