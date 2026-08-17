package com.akiba.backend.report.service;

import com.akiba.backend.notification.discord.DiscordNotificationService;
import com.akiba.backend.report.domain.Report;
import com.akiba.backend.report.domain.ReportType;
import com.akiba.backend.report.dto.request.ReportRequest;
import com.akiba.backend.report.dto.response.ReportResponse;
import com.akiba.backend.report.repository.ReportRepository;
import com.akiba.backend.user.domain.User;
import com.akiba.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final DiscordNotificationService discordNotificationService;

    public ReportResponse createReport(Long reporterId, ReportRequest request) {

        // 자기 자신 신고 방지
        if (reporterId.equals(request.getTargetUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 신고할 수 없습니다.");
        }

        // MARKET_POST 신고 시 targetPostId 필수
        if (request.getReportType() == ReportType.MARKET_POST && request.getTargetPostId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시글 신고 시 targetPostId는 필수입니다.");
        }
        // 중복 신고 방지
        // 중복 신고 방지
        if (request.getReportType() == ReportType.MARKET_POST) {
            if (reportRepository.existsByReporterIdAndTargetUserIdAndReportTypeAndTargetPostId(
                    reporterId, request.getTargetUserId(), request.getReportType(), request.getTargetPostId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 게시글입니다.");
            }
        } else {
            if (reportRepository.existsByReporterIdAndTargetUserIdAndReportType(
                    reporterId, request.getTargetUserId(), request.getReportType())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 대상입니다.");
            }
        }

        // 신고 대상 존재 확인
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetUserId(request.getTargetUserId())
                .reportType(request.getReportType())
                .reason(request.getReason())
                .detail(request.getDetail())
                .targetPostId(request.getTargetPostId())
                .build();

        Report saved = reportRepository.save(report);

        // Discord 알림
        String message = String.format("""
                🚨 **신규 신고 접수**
                
                📌 **신고 유형**: %s
                👤 **신고자**: %s (ID: %d)
                🎯 **신고 대상**: %s (ID: %d)
                📝 **신고 사유**: %s
                💬 **상세 내용**: %s
                🕐 **접수 시각**: %s
                """,
                saved.getReportType().name(),
                reporter.getNickname(), reporterId,
                targetUser.getNickname(), request.getTargetUserId(),
                saved.getReason(),
                saved.getDetail() != null ? saved.getDetail() : "없음",
                saved.getCreatedAt().toString()
        );
        discordNotificationService.send(message);

        return ReportResponse.builder()
                .reportId(saved.getReportId())
                .message("신고가 접수되었습니다.")
                .createdAt(saved.getCreatedAt())
                .build();
    }
}