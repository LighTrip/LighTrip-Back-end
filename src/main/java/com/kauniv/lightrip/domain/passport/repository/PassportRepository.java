// domain/passport/repository/PassportRepository.java
package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PassportRepository extends JpaRepository<Passport, Long> {

    // 중복 등록 방지 (uk_passport_user_location_date)
    boolean existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
            Long userId, BigDecimal latitude, BigDecimal longitude, LocalDate visitedAt
    );
}