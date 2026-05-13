// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/response/BidHistoryListResponse.java
// 설명: 경매 상세 페이지 > 입찰 내역 탭에서 표시할 전체 입찰 목록
//       총 입찰 수, 현재 최고가, 입찰 목록을 포함
// ========================================================================
package com.akiba.backend.auction.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;
import java.math.BigDecimal;

@Getter
@Builder
public class BidHistoryListResponse {

    private Long postId;                // 경매 게시글 ID
    private int totalBidCount;          // 총 입찰 수
    private BigDecimal currentHighestBid;      // 현재 최고 입찰가
    private BigDecimal startPrice;             // 경매 시작가
    private BigDecimal bidStep;                // 입찰 단위
    private BigDecimal nextMinBid;             // 다음 최소 입찰 가능 금액
    private List<BidItem> bids;         // 입찰 목록 (가격 내림차순)

    @Getter
    @Builder
    public static class BidItem {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private BigDecimal bidAmount;           // bidPrice → bidAmount
        private String createdAt;
        private boolean isHighest;    // 현재 최고가 입찰인지
    }
}