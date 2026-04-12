package com.kauniv.lightrip.domain.scrap.repository;

import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {
}