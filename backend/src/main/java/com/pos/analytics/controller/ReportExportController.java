package com.pos.analytics.controller;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.service.AnalyticsService;
import com.pos.analytics.service.PdfReportExportService;
import com.pos.analytics.util.HolidayUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 報表匯出控制器。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Log4j2
public class ReportExportController {

    private final AnalyticsService analyticsService;
    private final PdfReportExportService pdfService;

    /**
     * 匯出營運分析與雙 ORM 效能對比 PDF 報表。
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdfReport(@Valid @ModelAttribute ReportFilterRequest request) throws IOException {
        log.info("API 收到匯出 PDF 請求: {} ~ {}",
                HolidayUtils.sanitizeForLog(request.getStartDate()),
                HolidayUtils.sanitizeForLog(request.getEndDate()));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            log.warn("匯出請求失敗: 起始日期大於結束日期");
            return ResponseEntity.badRequest().build();
        }

        OrmComparisonResponse comparison = analyticsService.getComparedAnalytics(request);

        byte[] pdfBytes = pdfService.generatePdfReport(
                comparison.getData(),
                comparison.getMyBatisDurationMs(),
                comparison.getJpaDurationMs()
        );

        String safeFileName = String.format("pos-analytics-report-%s-to-%s.pdf",
                request.getStartDate(), request.getEndDate());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + "\"")
                .body(pdfBytes);
    }
}
