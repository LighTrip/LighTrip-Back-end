package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.domain.passport.entity.PassportImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PassportImageRepository extends JpaRepository<PassportImage, Long> {

    @Modifying
    @Query("DELETE FROM PassportImage pi WHERE pi.passport.id = :passportId")
    void deleteAllByPassportId(Long passportId);
}