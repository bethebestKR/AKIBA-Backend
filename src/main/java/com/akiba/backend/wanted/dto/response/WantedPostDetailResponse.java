// ========================================================================
// 파일 경로: com/akiba/backend/wanted/dto/response/WantedPostDetailResponse.java
// 설명: 구해요 상세 페이지 전체 정보
//       이미지, 작성자 정보, 찜 여부, 유사 구해요 글 포함
// ========================================================================
package com.akiba.backend.wanted.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.math.BigDecimal;

@Getter
@Builder
public class WantedPostDetailResponse {

    private Long postId;
    private String title;
    private String content;
    private BigDecimal price;
    private String conditionTxt;
    private String specialType;
    private String deliveryMethod;
    private String status;
    private int viewCount;
    private int favoriteCount;
    private boolean isFavorite;
    private LocalDateTime createdAt;

    // 이미지 목록
    private List<ImageResponse> images;

    // 작성자 정보
    private AuthorResponse author;

    // 유사 구해요 글
    private List<SimilarWantedResponse> similarPosts;

    @Getter
    @Builder
    public static class ImageResponse {
        private Long mediaId;
        private String imageUrl;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class AuthorResponse {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
    }

    @Getter
    @Builder
    public static class SimilarWantedResponse {
        private Long postId;
        private String title;
        private String conditionTxt;
        private LocalDateTime createdAt;
    }
}
