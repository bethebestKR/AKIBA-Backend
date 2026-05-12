// ========================================================================
// 파일 경로: com/akiba/backend/market/dto/response/MarketPostListResponse.java
// 설명: 중고거래 목록 조회 시 각 게시글 요약 정보 (카드 형태)
// ========================================================================
package com.akiba.backend.market.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Builder
public class MarketPostListResponse {

    private Long postId;
    private String type;              // USED, LIMITED
    private String title;
    private BigDecimal price;
    private String productCondition;
    private String specialType;
    private String status;
    private String thumbnailUrl;      // 첫 번째 이미지 URL
    private LocalDateTime createdAt;
    private int viewCount;
    private int favoriteCount;
}
