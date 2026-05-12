// ========================================================================
// 파일 경로: com/akiba/backend/market/dto/request/MarketPostStatusRequest.java
// 설명: 게시글 상태 변경 (판매중/예약중/판매완료 등)
// ========================================================================
package com.akiba.backend.market.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MarketPostStatusRequest {

    @NotBlank(message = "status 는 필수입니다.")
    @Pattern(regexp = "^(ACTIVE|RESERVED|SOLD|CLOSED|DELETED)$",
            message = "status 는 ACTIVE / RESERVED / SOLD / CLOSED / DELETED 중 하나여야 합니다.")
    private String status;
}