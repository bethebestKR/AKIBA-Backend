package com.akiba.backend.report.controller;

import com.akiba.backend.report.dto.request.ReportRequest;
import com.akiba.backend.report.dto.response.ReportResponse;
import com.akiba.backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "신고", description = "회원/게시글 신고")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "신고 접수")
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.ok(reportService.createReport(userId, request));
    }
}