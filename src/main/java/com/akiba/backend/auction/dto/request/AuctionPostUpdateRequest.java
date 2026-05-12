// ========================================================================
// 파일 경로: com/akiba/backend/auction/dto/request/AuctionPostUpdateRequest.java
// 설명: 경매 글 수정 (입찰이 없는 경우에만 수정 가능)
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
public class AuctionPostUpdateRequest {

    @NotBlank(message = "title은 필수입니다.")
    private String title;
    @NotBlank(message = "content는 필수입니다.")
    private String content;
    @NotBlank(message = "productCondition은 필수입니다.")
    @Pattern(regexp = "^(개봉|미개봉)$", message = "productCondition은 개봉/미개봉만 가능합니다.")
    private String productCondition;
    private String specialType; // NONE, SPECIAL_BENEFIT, LIMITED_EDITION, BOTH
    private Long categoryId;
    @NotNull(message = "startPrice는 필수입니다.")
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    private BigDecimal startPrice;

    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    private BigDecimal buyNowPrice; // 입찰 전에는 수정 가능

    @NotNull(message = "bidStep은 필수입니다.")
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 17, fraction = 2)
    private BigDecimal bidStep;
    @NotNull(message = "endsAt은 필수입니다.")
    private LocalDateTime endsAt;
    @NotBlank(message = "deliveryMethod는 필수입니다.")
    @Pattern(regexp = "^(택배|직거래)$", message = "deliveryMethod는 택배/직거래만 가능합니다.")
    private String deliveryMethod;
    @NotBlank(message = "purchaseSource는 필수입니다.")
    private String purchaseSource;
    @NotNull(message = "receiptMediaId는 필수입니다.")
    private Long receiptMediaId;
    @NotEmpty(message = "imageMediaIds는 최소 1개 이상 필요합니다.")
    private List<Long> imageMediaIds;
    private List<String> tagNames;
}
