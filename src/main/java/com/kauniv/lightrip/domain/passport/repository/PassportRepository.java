// PassportRepository.java
package com.kauniv.lightrip.domain.passport.repository;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportRepository extends JpaRepository<Passport, Long> {
}