package com.akiba.backend.market.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class RecentViewedPostResponse {
    private Long postId;
    private String title;
    private BigDecimal price;
    private String thumbnailUrl;
}
