// domain/scrap/repository/ScrapRepository.java
package com.kauniv.lightrip.domain.scrap.repository;

import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    // 여권 삭제 시 연관 스크랩 일괄 삭제 (성능 위해 bulk delete)
    @Modifying
    @Query("DELETE FROM Scrap s WHERE s.passport.id = :passportId")
    void deleteAllByPassportId(Long passportId);
}