// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/response/AuctionPostDetailResponse.java
// 설명: 경매 상세 페이지 전체 정보
//       입찰 내역, 판매자 정보, 이미지 등 포함
// ========================================================================
package com.akiba.backend.auction.dto.response;

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
public class AuctionPostDetailResponse {

    private Long postId;
    private String title;
    private String content;
    private String productCondition;
    private String specialType;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;       // 현재 최고 입찰가
    private BigDecimal buyNowPrice;
    private BigDecimal bidStep;
    private int bidCount;
    private String deliveryMethod;
    private String purchaseSource;
    private String status;
    private LocalDateTime endsAt;
    private int viewCount;
    private int favoriteCount;
    private boolean isFavorite;
    private boolean isMyPost;           // 내가 올린 경매인지
    private boolean hasBid;             // 내가 입찰했는지
    private LocalDateTime createdAt;

    // 이미지 목록
    private List<ImageResponse> images;

    // 태그 목록
    private List<String> tags;

    // 판매자 정보
    private SellerResponse seller;

    // 최근 입찰 내역 (최신 5건)
    private List<BidHistoryResponse> recentBids;

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
    }

    @Getter
    @Builder
    public static class BidHistoryResponse {
        private String bidderNickname;
        private BigDecimal bidAmount;
        private LocalDateTime bidAt;
    }
}
