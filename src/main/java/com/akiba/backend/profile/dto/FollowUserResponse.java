package com.akiba.backend.profile.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUserResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Double mannerScore;
}