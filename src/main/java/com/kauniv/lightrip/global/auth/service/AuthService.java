package com.kauniv.lightrip.global.auth.service;

import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.auth.repository.AuthRepository;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public String reissueAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        Auth auth = authRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Auth를 찾을 수 없습니다."));

        if (!auth.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 일치하지 않습니다.");
        }

        return jwtProvider.generateAccessToken(userId);
    }

    public void logout(Long userId) {
        Auth auth = authRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Auth를 찾을 수 없습니다."));

        auth.updateRefreshToken(null);
    }

    public void withdraw(Long userId) {
        authRepository.deleteByUser_Id(userId);
        userRepository.deleteById(userId);
    }
}