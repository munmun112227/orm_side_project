package com.pos.analytics;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.dto.ReportSummaryDto;
import com.pos.analytics.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 雙 ORM (JPA & MyBatis) 等價性測試。
 * 使用 @ActiveProfiles("test") 切換至 H2 記憶體資料庫與純人工合成脫敏資料 (Zero PII)，
 * 保證在 GitHub Actions CI 與本地無外部資料庫環境下 100% 獨立運行通過。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("雙 ORM (JPA & MyBatis) 脫敏測試環境等價性整合測試")
class DualOrmEquivalenceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("驗證雙 ORM 接續執行且指標結果完全一致 (使用脫敏合成資料)")
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

        // 驗證過濾邏輯：6 筆測試資料中，1 筆為 training，1 筆非 QPSCL/QPSCE 機台，有效共 4 筆
        assertThat(summary.getTotalAnalyzedCount()).isEqualTo(4L);
        assertThat(summary.getCashierTransactionCount()).isEqualTo(2L);
        assertThat(summary.getSelfCheckoutTransactionCount()).isEqualTo(2L);
        assertThat(summary.getSelfCheckoutUsageRate()).isEqualByComparingTo(new BigDecimal("50.00"));

        // 驗證客單價計算 (人工: 300/2=150, 自助: 200/2=100, 全體: 500/4=125)
        assertThat(summary.getCashierAverageTicket()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(summary.getSelfCheckoutAverageTicket()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(summary.getOverallAverageTicket()).isEqualByComparingTo(new BigDecimal("125.00"));

        System.out.printf("""
                ======================================================
                [脫敏資料雙 ORM 測試報告 (H2 In-Memory)]
                - 有效分析筆數:   %d 筆
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
