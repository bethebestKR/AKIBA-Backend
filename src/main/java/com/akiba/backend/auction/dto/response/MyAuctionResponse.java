// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/response/MyAuctionResponse.java
// 설명: 마이페이지 > 경매 관리에서 사용하는 공통 응답 DTO
//       내 입찰 현황, 내 경매 현황, 낙찰 성공 목록에서 공통 사용
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
public class MyAuctionResponse {

    private Long postId;
    private String title;
    private BigDecimal startPrice;
    private BigDecimal currentHighestBid;
    private BigDecimal buyNowPrice;
    private int bidCount;
    private Integer myBidPrice;        // 내 입찰가 (내 입찰 현황에서만)
    private BigDecimal finalPrice;        // 낙찰가 (낙찰 성공에서만)
    private String status;             // 경매 상태
    private LocalDateTime endsAt;
    private String thumbnailUrl;
}
