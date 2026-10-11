package com.pos.analytics.controller;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.service.AnalyticsService;
import com.pos.analytics.util.HolidayUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 營運數據與雙 ORM 效能比對 REST 控制器。
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Log4j2
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * 查詢營運報表指標並接續執行雙 ORM 進行效能對比。
     *
     * @param request 日期篩選區間 (經過 Jakarta Bean Validation 檢驗)
     * @return 雙 ORM 耗時、較快者結論與指標數據
     */
    @GetMapping("/compare")
    public ResponseEntity<OrmComparisonResponse> getComparedAnalytics(@Valid @ModelAttribute ReportFilterRequest request) {
        log.info("API 收到 GET /api/analytics/compare 請求, startDate={}, endDate={}",
                HolidayUtils.sanitizeForLog(request.getStartDate()),
                HolidayUtils.sanitizeForLog(request.getEndDate()));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            log.warn("請求日期區間非法: 起始日期大於結束日期");
            return ResponseEntity.badRequest().build();
        }

        OrmComparisonResponse response = analyticsService.getComparedAnalytics(request);
        return ResponseEntity.ok(response);
    }
}
