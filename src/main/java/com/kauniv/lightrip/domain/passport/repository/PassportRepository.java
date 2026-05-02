// domain/passport/repository/PassportRepository.java
package com.kauniv.lightrip.domain.passport.repository;
import com.kauniv.lightrip.global.enums.District;
import java.util.Optional;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PassportRepository extends JpaRepository<Passport, Long> {

    // 중복 등록 방지 (uk_passport_user_location_date)
    boolean existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
            Long userId, BigDecimal latitude, BigDecimal longitude, LocalDate visitedAt
    );

    // 해당 구에 사용자 여권이 있는지 확인
    boolean existsByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    // 해당 구의 가장 최근 여권 조회 (삭제 시 대표 이미지 교체용)
    Optional<Passport> findFirstByUser_IdAndDistrictCategoryOrderByCreatedAtDesc(
            Long userId, District districtCategory
    );

}