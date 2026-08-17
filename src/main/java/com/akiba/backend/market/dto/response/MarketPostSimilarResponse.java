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
public class MarketPostSimilarResponse {
    private Long postId;
    private String type;
    private String title;
    private BigDecimal price;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private int similarityScore;
    private List<String> reasonKeywords;
}

