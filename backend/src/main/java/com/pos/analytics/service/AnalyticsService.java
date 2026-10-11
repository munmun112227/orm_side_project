package com.pos.analytics.service;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.dto.ReportSummaryDto;
import com.pos.analytics.util.HolidayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 雙 ORM 效能對比與營運分析協調整合服務。
 * 接續執行 MyBatis 與 Spring Data JPA，精確量測執行耗時並比對數據一致性。
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class AnalyticsService {

    private final MyBatisAnalyticsService myBatisService;
    private final JpaAnalyticsService jpaService;

    /**
     * 執行雙 ORM 接續分析與效能耗時評測。
     *
     * @param request 報表日期區間過濾請求
     * @return 包含指標資料、ORM 耗時與對比摘要的回應物件
     */
    public OrmComparisonResponse getComparedAnalytics(ReportFilterRequest request) {
        log.info(">>>>> [ORM Benchmark] 收到營運報表查詢請求: {} ~ {} <<<<<",
                HolidayUtils.sanitizeForLog(request.getStartDate()),
                HolidayUtils.sanitizeForLog(request.getEndDate()));

        // 1. 執行 MyBatis
        log.info("[ORM Benchmark] 階段 1/2: 開始執行 MyBatis 查詢...");
        long startMyBatis = System.nanoTime();
        ReportSummaryDto myBatisResult = myBatisService.calculateSummary(request);
        long myBatisDurationMs = (System.nanoTime() - startMyBatis) / 1_000_000;
        log.info("[ORM Benchmark] MyBatis 執行完成，耗時: {} ms, 統計交易總數: {}",
                myBatisDurationMs, myBatisResult.getTotalAnalyzedCount());

        // 2. 接續執行 Spring Data JPA
        log.info("[ORM Benchmark] 階段 2/2: 開始接續執行 Spring Data JPA 查詢...");
        long startJpa = System.nanoTime();
        ReportSummaryDto jpaResult = jpaService.calculateSummary(request);
        long jpaDurationMs = (System.nanoTime() - startJpa) / 1_000_000;
        log.info("[ORM Benchmark] Spring Data JPA 執行完成，耗時: {} ms, 統計交易總數: {}",
                jpaDurationMs, jpaResult.getTotalAnalyzedCount());

        // 3. 雙 ORM 資料等價性驗證 (Data Consistency Verification)
        boolean isConsistent = checkDataConsistency(myBatisResult, jpaResult);
        log.info("[ORM Benchmark] 雙 ORM 數據一致性檢查結果: {}", isConsistent ? "通過 (一致)" : "警告 (不一致)");

        // 4. 判定效能較快者與計算比率
        String fasterOrm;
        String speedComparison;
        if (myBatisDurationMs < jpaDurationMs) {
            fasterOrm = "MyBatis";
            double diff = jpaDurationMs > 0 ? ((double) (jpaDurationMs - myBatisDurationMs) / jpaDurationMs) * 100 : 0;
            speedComparison = String.format("MyBatis 較 JPA 快 %d ms (提升約 %.1f%%)",
                    (jpaDurationMs - myBatisDurationMs), diff);
        } else if (jpaDurationMs < myBatisDurationMs) {
            fasterOrm = "Spring Data JPA";
            double diff = myBatisDurationMs > 0 ? ((double) (myBatisDurationMs - jpaDurationMs) / myBatisDurationMs) * 100 : 0;
            speedComparison = String.format("Spring Data JPA 較 MyBatis 快 %d ms (提升約 %.1f%%)",
                    (myBatisDurationMs - jpaDurationMs), diff);
        } else {
            fasterOrm = "平手 (Tie)";
            speedComparison = "兩者執行耗時相同";
        }

        log.info("[ORM Benchmark] 比對結論: 更快的 ORM 為 [{}], {}", fasterOrm, speedComparison);

        return OrmComparisonResponse.builder()
                .data(myBatisResult)
                .myBatisDurationMs(myBatisDurationMs)
                .jpaDurationMs(jpaDurationMs)
                .fasterOrm(fasterOrm)
                .speedComparisonSummary(speedComparison)
                .dataConsistent(isConsistent)
                .build();
    }

    private boolean checkDataConsistency(ReportSummaryDto a, ReportSummaryDto b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getCashierTransactionCount(), b.getCashierTransactionCount())
                && Objects.equals(a.getSelfCheckoutTransactionCount(), b.getSelfCheckoutTransactionCount())
                && Objects.equals(a.getTotalAnalyzedCount(), b.getTotalAnalyzedCount())
                && Objects.equals(a.getSelfCheckoutUsageRate(), b.getSelfCheckoutUsageRate())
                && Objects.equals(a.getCashierAverageTicket(), b.getCashierAverageTicket())
                && Objects.equals(a.getSelfCheckoutAverageTicket(), b.getSelfCheckoutAverageTicket())
                && Objects.equals(a.getOverallAverageTicket(), b.getOverallAverageTicket());
    }
}
