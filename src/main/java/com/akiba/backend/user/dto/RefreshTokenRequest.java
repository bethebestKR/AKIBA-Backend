package com.akiba.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken 은 필수입니다.")
    private String refreshToken;
}