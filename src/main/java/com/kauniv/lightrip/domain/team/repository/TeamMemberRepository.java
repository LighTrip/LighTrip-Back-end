package com.kauniv.lightrip.domain.team.repository;

import com.kauniv.lightrip.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeam_IdAndUser_Id(Long teamId, Long userId);

    List<TeamMember> findAllByTeam_Id(Long teamId);

    Optional<TeamMember> findByTeam_IdAndUser_Id(Long teamId, Long userId);

    Optional<TeamMember> findByTeam_IdAndRole(Long teamId, TeamMember.Role role);

    // 유저가 속한 팀 조회 (유저당 팀 1개)
    Optional<TeamMember> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    // 탈퇴 시 소속된 모든 팀 정리용 (유저당 팀 1개가 정상이지만, 과거 데이터 정합성 깨짐에 대비해 List로 조회)
    List<TeamMember> findAllByUser_Id(Long userId);

    @Modifying
    @Query("DELETE FROM TeamMember tm WHERE tm.team.id = :teamId")
    void deleteAllByTeam_Id(@Param("teamId") Long teamId);
}