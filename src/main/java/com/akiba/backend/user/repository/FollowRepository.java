package com.akiba.backend.user.repository;

import com.akiba.backend.user.domain.Follow;
import com.akiba.backend.user.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    List<Follow> findByFollowerId(Long followerId);
    List<Follow> findByFollowingId(Long followingId);
}
