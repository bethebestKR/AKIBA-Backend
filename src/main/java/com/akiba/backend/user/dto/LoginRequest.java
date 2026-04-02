package com.akiba.backend.user.dto;

import lombok.Getter;

@Getter
public class LoginRequest {
    private String provider;
    private String code;
    private String state;
    private String env; // "dev" or "prod"
}