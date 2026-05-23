package com.kauniv.lightrip.domain.team.repository;

import com.kauniv.lightrip.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeam_IdAndUser_Id(Long teamId, Long userId);

    List<TeamMember> findAllByTeam_Id(Long teamId);

    Optional<TeamMember> findByTeam_IdAndUser_Id(Long teamId, Long userId);

    Optional<TeamMember> findByTeam_IdAndRole(Long teamId, TeamMember.Role role);

    // 유저가 속한 팀 조회 (유저당 팀 1개)
    Optional<TeamMember> findByUser_Id(Long userId);
}