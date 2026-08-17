// ========================================================================
// 파일 경로: com/akiba/backend/wanted/dto/response/WantedPostListResponse.java
// 설명: 구해요 목록 페이지 카드에 표시할 요약 정보
// ========================================================================
package com.akiba.backend.wanted.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Builder
public class WantedPostListResponse {

    private Long postId;
    private String title;
    private String contentPreview;       // 내용 미리보기 (앞 50자)
    private BigDecimal price;
    private String conditionTxt;
    private String specialType;
    private String deliveryMethod;
    private String authorNickname;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private int viewCount;
    private int favoriteCount;
}
