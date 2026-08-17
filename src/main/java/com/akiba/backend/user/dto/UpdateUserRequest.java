package com.akiba.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateUserRequest {

    // 닉네임은 선택 필드 (변경 안 할 수도)
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]*$",
            message = "닉네임은 한글/영문/숫자/_ 만 사용 가능합니다.")
    private String nickname;

    @Size(max = 200, message = "자기소개는 최대 200자입니다.")
    private String bio;

    private Long profileImageMediaId;
}