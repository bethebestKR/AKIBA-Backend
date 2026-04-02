package com.akiba.backend.user.service;

import com.akiba.backend.config.exception.TokenExpiredException;
import com.akiba.backend.config.jwt.TokenProvider;
import com.akiba.backend.user.domain.AuthProvider;
import com.akiba.backend.user.domain.RefreshToken;
import com.akiba.backend.user.domain.User;
import com.akiba.backend.user.domain.UserProfile;
import com.akiba.backend.user.dto.*;
import com.akiba.backend.user.repository.RefreshTokenRepository;
import com.akiba.backend.user.repository.UserProfileRepository;
import com.akiba.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${NAVER_CLIENT_ID_DEV}")
    private String clientIdDev;

    @Value("${NAVER_CLIENT_SECRET_DEV}")
    private String clientSecretDev;

    @Value("${NAVER_CLIENT_ID_PROD}")
    private String clientIdProd;

    @Value("${NAVER_CLIENT_SECRET_PROD}")
    private String clientSecretProd;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 인가 코드로 네이버에 액세스 토큰 요청
        String resolvedClientId = "prod".equals(request.getEnv()) ? clientIdProd : clientIdDev;
        String resolvedClientSecret = "prod".equals(request.getEnv()) ? clientSecretProd : clientSecretDev;
        String naverAccessToken = getNaverAccessToken(request.getCode(), request.getState(), resolvedClientId, resolvedClientSecret);

        // 2. 액세스 토큰으로 유저 정보 요청
        Map<String, Object> userInfo = getNaverUserInfo(naverAccessToken);
        Map<String, Object> response = (Map<String, Object>) userInfo.get("response");

        String oauthId = (String) response.get("id");
        String email = (String) response.get("email");
        String nickname = (String) response.get("nickname");

        // 3. 신규 유저인지 확인
        boolean isNewUser = !userRepository.findByProviderAndOauthId(AuthProvider.NAVER, oauthId).isPresent();

        User user = userRepository.findByProviderAndOauthId(AuthProvider.NAVER, oauthId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .provider(AuthProvider.NAVER)
                            .oauthId(oauthId)
                            .nickname(nickname)
                            .build();
                    User savedUser = userRepository.save(newUser);

                    UserProfile profile = UserProfile.builder()
                            .user(savedUser)
                            .build();
                    userProfileRepository.save(profile);

                    return savedUser;
                });

        // 4. JWT 토큰 발급
        String accessToken = tokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        // 5. refreshToken DB 저장 (있으면 업데이트, 없으면 새로 저장)
        refreshTokenRepository.findByUserId(user.getUserId())
                .ifPresentOrElse(
                        token -> token.update(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(user.getUserId(), refreshToken))
                );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .isNewUser(isNewUser)
                .build();


    }

    private String getNaverAccessToken(String code, String state, String clientId, String clientSecret) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://nid.naver.com/oauth2.0/token"
                + "?grant_type=authorization_code"
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&code=" + code
                + "&state=" + state;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return (String) response.getBody().get("access_token");
    }

    private Map<String, Object> getNaverUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                entity,
                Map.class
        );
        return response.getBody();
    }


    //닉네임 변경 ->DB에 닉네임 저장
    @Transactional
    public NicknameResponse updateNickname(Long userId, NicknameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        user.updateNickname(request.getNickname());

        return NicknameResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .build();
    }

    //닉네임 중복 체크
    public boolean checkNickname(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // 회원탈퇴
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        user.delete();
    }

    public UserInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("프로필을 찾을 수 없습니다."));

        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .bio(profile.getBio())
                .profileImageMediaId(profile.getProfileImageMediaId())
                .build();
    }

    //회원정보 수정
    @Transactional
    public UpdateUserResponse updateMyInfo(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("프로필을 찾을 수 없습니다."));

        if (request.getNickname() != null) {
            user.updateNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            profile.updateBio(request.getBio());
        }
        if (request.getProfileImageMediaId() != null) {
            profile.updateProfileImage(request.getProfileImageMediaId());
        }

        return UpdateUserResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .bio(profile.getBio())
                .profileImageMediaId(profile.getProfileImageMediaId())
                .message("회원정보 수정 성공")
                .build();
    }

    //리프레시 토큰
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        // 1 리프레시 토큰 유효성 검증
        if (!tokenProvider.validToken(request.getRefreshToken())) {
            throw new TokenExpiredException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }
        // 2 리프레시 토큰으로 userId 추출
        Long userId = tokenProvider.getUserId(request.getRefreshToken());
        // 3 DB에 저장된 리프레시 토큰과 비교
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("리프레시 토큰을 찾을 수 없습니다."));

        if (!refreshToken.getRefreshToken().equals(request.getRefreshToken())) {
            throw new RuntimeException("리프레시 토큰이 일치하지 않습니다.");
        }

        // 4 새 액세스 토큰 발급
        String newAccessToken = tokenProvider.generateAccessToken(userId);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }



}