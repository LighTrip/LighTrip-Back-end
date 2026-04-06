package com.kauniv.lightrip.global.auth.repository;

import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.oauth.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {

    Optional<Auth> findBySocialIdAndSocialType(String socialId, SocialType socialType);
    Optional<Auth> findByUser_Id(Long userId);
    Optional<Auth> findByUser_IdAndSocialType(Long userId, SocialType socialType);

    void deleteByUser_Id(Long userId);

}