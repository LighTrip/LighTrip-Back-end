package com.kauniv.lightrip.domain.like.repository;

import com.kauniv.lightrip.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {

    long countByUser_Id(Long userId);
}