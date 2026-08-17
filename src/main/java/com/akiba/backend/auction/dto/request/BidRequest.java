// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/request/BidRequest.java
// 설명: 입찰 시 프론트에서 보내는 데이터
// ========================================================================
package com.akiba.backend.auction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class BidRequest {

    @NotNull(message = "bidAmount 는 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "bidAmount 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2, message = "bidAmount 형식이 올바르지 않습니다.")
    private BigDecimal bidAmount;  // 입찰 금액
}