package com.kauniv.lightrip.domain.passport.entity;

import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.global.enums.District;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "district_cover",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_district_cover_user_district",
                        columnNames = {"user_id", "district_category"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DistrictCover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cover_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;
    // > 커버 대표 이미지 URL. 여권 이미지 URL을 복사하거나, 사용자가 자유롭게 외부 URL로 변경 가능.

    @Enumerated(EnumType.STRING)
    @Column(name = "district_category", nullable = false)
    private District districtCategory;

    @Column(name = "text_color", nullable = false, length = 7)
    @Builder.Default
    private String textColor = "#FFFFFF";
    // > 커버 이미지 위에 표시되는 지역명 텍스트 색상 (HEX #RRGGBB).
    // > 기본값 흰색. PATCH API로 사용자가 변경.

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void changeCoverImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void changeTextColor(String textColor) {
        this.textColor = textColor;
    }
}