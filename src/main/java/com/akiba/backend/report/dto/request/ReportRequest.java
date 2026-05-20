package com.akiba.backend.report.dto.request;

import com.akiba.backend.report.domain.ReportType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {

    @NotNull
    private Long targetUserId;

    @NotNull
    private ReportType reportType;

    @NotBlank
    @Size(max = 200)
    private String reason;

    @Size(max = 500)
    private String detail;

    private Long targetPostId;
}