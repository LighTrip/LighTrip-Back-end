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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_image_id", nullable = false)
    private PassportImage passportImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "district_category", nullable = false)
    private District districtCategory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void changeCoverImage(PassportImage newImage) {
        this.passportImage = newImage;
    }
}