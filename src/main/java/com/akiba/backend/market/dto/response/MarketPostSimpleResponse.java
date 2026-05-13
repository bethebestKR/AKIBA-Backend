// ========================================================================
// 파일 경로: com/akiba/backend/market/dto/response/MarketPostSimpleResponse.java
// 설명: 인기 매물, 유사 상품, 최근 본 상품 등 간략한 카드용 응답
// ========================================================================
package com.akiba.backend.market.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class MarketPostSimpleResponse {

    private Long postId;
    private String title;
    private BigDecimal price;
    private String thumbnailUrl;
    private String type;
    private String specialType;
}
