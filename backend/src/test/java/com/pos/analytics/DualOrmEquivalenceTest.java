package com.pos.analytics;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.dto.ReportSummaryDto;
import com.pos.analytics.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("雙 ORM (JPA & MyBatis) 真實資料查詢與等價性整合測試")
class DualOrmEquivalenceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("驗證雙 ORM 接續執行且指標結果完全一致")
    void testDualOrmEquivalence() {
        ReportFilterRequest request = ReportFilterRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        OrmComparisonResponse response = analyticsService.getComparedAnalytics(request);

        assertThat(response).isNotNull();
        assertThat(response.getMyBatisDurationMs()).isNotNull();
        assertThat(response.getJpaDurationMs()).isNotNull();
        assertThat(response.getDataConsistent()).isTrue();

        ReportSummaryDto summary = response.getData();
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalAnalyzedCount()).isGreaterThan(0L);
        assertThat(summary.getSelfCheckoutUsageRate()).isNotNull();
        assertThat(summary.getCashierAverageTicket()).isNotNull();
        assertThat(summary.getSelfCheckoutAverageTicket()).isNotNull();
        assertThat(summary.getOverallAverageTicket()).isNotNull();

        System.out.printf("""
                ======================================================
                [雙 ORM 效能對比測試報告]
                - 統計交易總數: %d 筆
                - 自助結帳機使用率: %s %%
                - 客單價 (人工機): $ %s
                - 客單價 (自助機): $ %s
                - 客單價 (全體):   $ %s
                - MyBatis 耗時:   %d ms
                - JPA 耗時:       %d ms
                - 獲勝 ORM:       %s (%s)
                - 數據一致性:     %s
                ======================================================
                %n""",
                summary.getTotalAnalyzedCount(),
                summary.getSelfCheckoutUsageRate(),
                summary.getCashierAverageTicket(),
                summary.getSelfCheckoutAverageTicket(),
                summary.getOverallAverageTicket(),
                response.getMyBatisDurationMs(),
                response.getJpaDurationMs(),
                response.getFasterOrm(),
                response.getSpeedComparisonSummary(),
                response.getDataConsistent() ? "通過" : "不通過"
        );
    }
}
