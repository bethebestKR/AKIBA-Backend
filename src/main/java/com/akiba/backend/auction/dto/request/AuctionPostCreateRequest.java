// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/request/AuctionPostCreateRequest.java
// 설명: 경매 게시글 작성 시 프론트에서 보내는 데이터
//       MarketPost(type=AUCTION) + AuctionPost 동시 생성
// ========================================================================
package com.akiba.backend.auction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class AuctionPostCreateRequest {

    @NotBlank(message = "title은 필수입니다.")
    private String title;              // 제목
    @NotBlank(message = "content는 필수입니다.")
    private String content;            // 설명
    @NotBlank(message = "productCondition은 필수입니다.")
    @Pattern(regexp = "^(개봉|미개봉)$", message = "productCondition은 개봉/미개봉만 가능합니다.")
    private String productCondition;
    private String specialType;        // NONE, SPECIAL_BENEFIT, LIMITED_EDITION, BOTH
    private Long categoryId;           // 카테고리 (선택)
    @NotNull(message = "startPrice는 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "startPrice 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal startPrice;        // 시작가

    @DecimalMin(value = "0", inclusive = false, message = "buyNowPrice 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal buyNowPrice;       // 즉시구매가 (선택)

    @NotNull(message = "bidStep은 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "bidStep 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal bidStep;           // 입찰 단위
    @NotNull(message = "endsAt은 필수입니다.")
    private LocalDateTime endsAt;      // 경매 종료 시간
    @NotBlank(message = "deliveryMethod는 필수입니다.")
    @Pattern(regexp = "^(택배|직거래)$", message = "deliveryMethod는 택배/직거래만 가능합니다.")
    private String deliveryMethod;     // 거래 방식
    @NotBlank(message = "purchaseSource는 필수입니다.")
    private String purchaseSource;     // 구매처 (필수)
    @NotNull(message = "receiptMediaId는 필수입니다.")
    private Long receiptMediaId;       // 영수증 이미지 (필수)
    @NotEmpty(message = "imageMediaIds는 최소 1개 이상 필요합니다.")
    private List<Long> imageMediaIds;  // 이미지 목록
    private List<String> tagNames;     // 태그 (최대 5개)
}
