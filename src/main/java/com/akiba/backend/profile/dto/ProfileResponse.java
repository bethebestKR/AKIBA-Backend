package com.akiba.backend.profile.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {
    private Long userId;
    private String nickname;
    private String bio;
    private String profileImageUrl;
    private Double mannerScore;
    private Integer ongoingDealCount;
    private Long followerCount;
    private Long followingCount;
    private Boolean isFollowing;

}