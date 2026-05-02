package com.kauniv.lightrip.domain.team.repository;

import com.kauniv.lightrip.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}