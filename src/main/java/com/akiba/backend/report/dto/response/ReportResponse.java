package com.akiba.backend.report.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {
    private Long reportId;
    private String message;
    private LocalDateTime createdAt;
}