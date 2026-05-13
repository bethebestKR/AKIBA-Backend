// ========================================================================
// 파일 경로: com/akiba/backend/wanted/dto/request/WantedPostCreateRequest.java
// 설명: 구해요 게시글 작성 시 프론트에서 보내는 데이터
//       MarketPost(type=WANTED) + WantedPost 동시 생성에 필요한 필드
// ========================================================================
package com.akiba.backend.wanted.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class WantedPostCreateRequest {

    @NotBlank(message = "title은 필수입니다.")
    private String title;              // 제목
    @NotBlank(message = "content는 필수입니다.")
    private String content;            // 설명
    @NotNull(message = "price는 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "price 는 0 보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal price;             // 희망 가격
    private String specialType;        // NONE, SPECIAL_BENEFIT, LIMITED_EDITION, BOTH
    @NotBlank(message = "conditionTxt는 필수입니다.")
    @Pattern(regexp = "^(개봉|미개봉)$", message = "conditionTxt는 개봉/미개봉만 가능합니다.")
    private String conditionTxt;       // 희망 상태 ("개봉", "미개봉")
    @NotBlank(message = "deliveryMethod는 필수입니다.")
    @Pattern(regexp = "^(택배|직거래)$", message = "deliveryMethod는 택배/직거래만 가능합니다.")
    private String deliveryMethod;     // 거래 방식 ("택배", "직거래")
    private List<Long> imageMediaIds;  // 이미지 (선택)
    @Size(max = 5, message = "tagNames는 최대 5개입니다.")
    private List<String> tagNames;     // 태그 (최대 5개)
}
