package com.akiba.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotBlank(message = "provider 는 필수입니다.")
    private String provider;

    @NotBlank(message = "code 는 필수입니다.")
    private String code;

    @NotBlank(message = "state 는 필수입니다.")
    private String state;

    @Pattern(regexp = "^(dev|prod)?$", message = "env 는 dev 또는 prod 여야 합니다.")
    private String env; // "dev" or "prod"
}