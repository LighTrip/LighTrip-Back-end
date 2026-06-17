package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.global.enums.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DistrictCoverRepository extends JpaRepository<DistrictCover, Long> {

    // 개인 커버 (team_id IS NULL): 팀 커버는 user_id가 null이므로 자동으로 제외됨
    Optional<DistrictCover> findByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    boolean existsByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    void deleteByUser_IdAndDistrictCategory(Long userId, District districtCategory);

    List<DistrictCover> findAllByUser_Id(Long userId);

    // 팀 커버 (team_id = teamId)
    Optional<DistrictCover> findByTeam_IdAndDistrictCategory(Long teamId, District districtCategory);

    boolean existsByTeam_IdAndDistrictCategory(Long teamId, District districtCategory);

    List<DistrictCover> findAllByTeam_Id(Long teamId);
}