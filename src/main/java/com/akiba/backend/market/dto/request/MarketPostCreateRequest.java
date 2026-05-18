// ========================================================================
// 파일 경로: com/akiba/backend/market/dto/request/MarketPostCreateRequest.java
// 설명: 중고거래/특전한정판 게시글 작성 시 프론트에서 보내는 데이터
// ========================================================================
package com.akiba.backend.market.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class MarketPostCreateRequest {

    @NotBlank(message = "type은 필수입니다.")
    private String type;              // "USED" 또는 "LIMITED"
    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 200, message = "title은 최대 200자입니다.")
    private String title;             // 제목
    @NotBlank(message = "content는 필수입니다.")
    private String content;           // 내용
    @NotNull(message = "price는 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "price 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal price;            // 가격
    @NotBlank(message = "productCondition은 필수입니다.")
    @Pattern(regexp = "^(개봉|미개봉)$", message = "productCondition은 개봉/미개봉만 가능합니다.")
    private String productCondition;
    private String specialType;        // NONE, SPECIAL_BENEFIT, LIMITED_EDITION, BOTH
    private Long categoryId;          // 카테고리 ID (선택)
    @NotBlank(message = "deliveryMethod는 필수입니다.")
    @Pattern(regexp = "^(택배|직거래)$", message = "deliveryMethod는 택배/직거래만 가능합니다.")
    private String deliveryMethod;    // 거래 방식: "택배", "직거래"
    private String purchaseSource;    // 구매처 (선택)
    private Long receiptMediaId;      // 영수증 이미지 (선택)
    @NotEmpty(message = "imageMediaIds는 최소 1개 이상 필요합니다.")
    private List<Long> imageMediaIds; // 이미지 미디어 ID 목록
    @Size(max = 5, message = "tagNames는 최대 5개입니다.")
    private List<String> tagNames;    // 태그 이름 목록 (최대 5개)
}
