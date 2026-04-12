// RankingRepository.java
package com.kauniv.lightrip.domain.ranking.repository;

import com.kauniv.lightrip.domain.ranking.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRepository extends JpaRepository<Ranking, Long> {
}