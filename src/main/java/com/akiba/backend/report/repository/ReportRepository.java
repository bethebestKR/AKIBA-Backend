package com.akiba.backend.report.repository;

import com.akiba.backend.report.domain.Report;
import com.akiba.backend.report.domain.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // MEMBER, CHAT 신고 중복 체크
    boolean existsByReporterIdAndTargetUserIdAndReportType(
            Long reporterId, Long targetUserId, ReportType reportType
    );
    // MARKET_POST 신고 중복 체크
    boolean existsByReporterIdAndTargetUserIdAndReportTypeAndTargetPostId(
            Long reporterId, Long targetUserId, ReportType reportType, Long targetPostId
    );
}