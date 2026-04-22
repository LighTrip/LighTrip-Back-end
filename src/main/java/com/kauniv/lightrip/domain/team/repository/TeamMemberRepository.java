package com.kauniv.lightrip.domain.team.repository;

import com.kauniv.lightrip.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeam_IdAndUser_Id(Long teamId, Long userId);
}