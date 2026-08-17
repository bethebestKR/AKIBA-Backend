package com.akiba.backend.profile.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class FollowListResponse {
    private List<FollowUserResponse> followings;
    private int totalCount;
}