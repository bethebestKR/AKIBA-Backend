package com.akiba.backend.profile.controller;

import com.akiba.backend.profile.dto.FollowListResponse;
import com.akiba.backend.profile.dto.FollowResponse;
import com.akiba.backend.profile.dto.ProfileResponse;
import com.akiba.backend.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(profileService.getUserProfile(userId,userId));
    }

    @GetMapping("/{targetId}")
    public ResponseEntity<ProfileResponse> getUserProfile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetId) {
        return ResponseEntity.ok(profileService.getUserProfile(userId, targetId));
    }

    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowResponse> follow(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetId) {
        return ResponseEntity.ok(profileService.follow(userId, targetId));
    }

    @DeleteMapping("/{targetId}/unfollow")
    public ResponseEntity<FollowResponse> unfollow(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetId) {
        return ResponseEntity.ok(profileService.unfollow(userId, targetId));
    }

    @GetMapping("/followings")
    public ResponseEntity<FollowListResponse> getFollowings(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(profileService.getFollowings(userId));
    }

    @GetMapping("/followers")
    public ResponseEntity<FollowListResponse> getFollowers(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(profileService.getFollowers(userId));
    }
}