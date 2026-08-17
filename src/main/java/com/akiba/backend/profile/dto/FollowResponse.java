package com.akiba.backend.profile.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowResponse {
    private String message;
    private Long targetId;
}