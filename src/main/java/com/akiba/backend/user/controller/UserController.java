package com.akiba.backend.user.controller;

import com.akiba.backend.user.dto.*;
import com.akiba.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.akiba.backend.config.jwt.TokenProvider;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<NicknameResponse> updateNickname(
            @Valid @RequestBody NicknameRequest request,
            @AuthenticationPrincipal Long userId) {
        NicknameResponse response = userService.updateNickname(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(@RequestParam String nickname) {
        boolean available = userService.checkNickname(nickname);
        String message = available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.";
        return ResponseEntity.ok(Map.of("available", available, "message", message));
    }

    //탈퇴
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteUser(@AuthenticationPrincipal Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "회원탈퇴가 완료되었습니다."));
    }


    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMyInfo(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateUserResponse> updateMyInfo(@AuthenticationPrincipal Long userId,
                                                           @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMyInfo(userId, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refresh(request));
    }

    @PostMapping("/test-login")
    public ResponseEntity<LoginResponse> testLogin(@RequestParam Long userId) {
        String accessToken = tokenProvider.generateAccessToken(userId);
        String refreshToken = tokenProvider.generateRefreshToken(userId);

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .nickname("테스트유저")
                .isNewUser(false)
                .build());
    }
}