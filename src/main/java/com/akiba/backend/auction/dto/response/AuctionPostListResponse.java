// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/response/AuctionPostListResponse.java
// 설명: 경매 목록 페이지 카드에 표시할 요약 정보
//       남은 시간, 현재 최고가, 입찰 수 포함
// ========================================================================
package com.akiba.backend.auction.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Builder
public class AuctionPostListResponse {

    private Long postId;
    private String title;
    private String specialType;
    private BigDecimal startPrice;            // 시작가
    private BigDecimal currentPrice;      // 현재 최고 입찰가
    private BigDecimal buyNowPrice;       // 즉시구매가
    private int bidCount;              // 입찰 수
    private String thumbnailUrl;
    private LocalDateTime endsAt;      // 경매 종료 시간
    private String status;             // ACTIVE, ENDED, SOLD
    private int viewCount;
    private int favoriteCount;
}
