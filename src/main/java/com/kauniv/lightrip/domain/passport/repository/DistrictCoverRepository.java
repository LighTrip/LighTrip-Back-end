package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.global.enums.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DistrictCoverRepository extends JpaRepository<DistrictCover, Long> {

    Optional<DistrictCover> findByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    boolean existsByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    void deleteByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    List<DistrictCover> findAllByUser_Id(Long userId);
}