// ========================================================================
// 파일 경로: com/akiba/backend/market/dto/response/MarketPostDetailResponse.java
// 설명: 중고거래 상세 페이지에서 보여줄 전체 정보
//       이미지 목록, 태그, 판매자 정보, 찜 여부 포함
// ========================================================================
package com.akiba.backend.market.dto.response;

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
public class MarketPostDetailResponse {

    private Long postId;
    private String type;
    private String title;
    private String content;
    private BigDecimal price;
    private String productCondition;
    private String specialType;
    private String status;
    private String deliveryMethod;
    private String purchaseSource;
    private Long receiptMediaId;
    private int viewCount;
    private int favoriteCount;
    private boolean isFavorite;       // 로그인 유저가 찜했는지
    private LocalDateTime createdAt;

    // 이미지 목록
    private List<ImageResponse> images;

    // 태그 목록
    private List<String> tags;

    // 판매자 정보
    private SellerResponse seller;

    // ----- 내부 DTO -----

    @Getter
    @Builder
    public static class ImageResponse {
        private Long mediaId;
        private String imageUrl;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class SellerResponse {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String bio;
        private int dealCount;
        private int reviewCount;
    }
}
