package com.akiba.backend.used.dto.request;

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
public class UsedPostUpdateRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 200, message = "title은 최대 200자입니다.")
    private String title;

    @NotBlank(message = "content는 필수입니다.")
    private String content;

    @NotNull(message = "price는 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "price 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal price;

    @NotBlank(message = "productCondition은 필수입니다.")
    @Pattern(regexp = "^(개봉|미개봉)$", message = "productCondition은 개봉/미개봉만 가능합니다.")
    private String productCondition;
    private String specialType; // NONE, SPECIAL_BENEFIT, LIMITED_EDITION, BOTH

    private Long categoryId;

    @NotBlank(message = "deliveryMethod는 필수입니다.")
    @Pattern(regexp = "^(택배|직거래)$", message = "deliveryMethod는 택배/직거래만 가능합니다.")
    private String deliveryMethod;

    private String purchaseSource;
    private Long receiptMediaId;

    @NotEmpty(message = "imageMediaIds는 최소 1개 이상 필요합니다.")
    private List<Long> imageMediaIds;

    @Size(max = 5, message = "tagNames는 최대 5개입니다.")
    private List<String> tagNames;
}
