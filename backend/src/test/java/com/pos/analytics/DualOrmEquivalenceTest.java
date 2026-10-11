package com.pos.analytics;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.dto.ReportSummaryDto;
import com.pos.analytics.service.AnalyticsService;
import com.pos.analytics.service.JpaAnalyticsService;
import com.pos.analytics.service.MyBatisAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 雙 ORM (JPA & MyBatis) 等價性與效能分析單元測試。
 * 採用純單元測試 (Mockito)，完全解耦實體資料庫，確保在無 DB 環境與 GitHub CI 下 100% 穩定且極速運行。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("雙 ORM (JPA & MyBatis) 等價性純單元測試")
class DualOrmEquivalenceTest {

    @Mock
    private JpaAnalyticsService jpaAnalyticsService;

    @Mock
    private MyBatisAnalyticsService myBatisAnalyticsService;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("驗證雙 ORM 接續執行且指標結果完全一致 (純單元測試，零 DB 依賴)")
    void testDualOrmEquivalence() {
        ReportFilterRequest request = ReportFilterRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        ReportSummaryDto mockSummary = ReportSummaryDto.builder()
                .totalAnalyzedCount(4L)
                .cashierTransactionCount(2L)
                .selfCheckoutTransactionCount(2L)
                .selfCheckoutUsageRate(new BigDecimal("50.00"))
                .cashierAverageTicket(new BigDecimal("150.00"))
                .selfCheckoutAverageTicket(new BigDecimal("100.00"))
                .overallAverageTicket(new BigDecimal("125.00"))
                .build();

        when(myBatisAnalyticsService.calculateSummary(any())).thenReturn(mockSummary);
        when(jpaAnalyticsService.calculateSummary(any())).thenReturn(mockSummary);

        OrmComparisonResponse response = analyticsService.getComparedAnalytics(request);

        assertThat(response).isNotNull();
        assertThat(response.getMyBatisDurationMs()).isNotNull();
        assertThat(response.getJpaDurationMs()).isNotNull();
        assertThat(response.getDataConsistent()).isTrue();

        ReportSummaryDto summary = response.getData();
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalAnalyzedCount()).isEqualTo(4L);
        assertThat(summary.getCashierTransactionCount()).isEqualTo(2L);
        assertThat(summary.getSelfCheckoutTransactionCount()).isEqualTo(2L);
        assertThat(summary.getSelfCheckoutUsageRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(summary.getCashierAverageTicket()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(summary.getSelfCheckoutAverageTicket()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(summary.getOverallAverageTicket()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("驗證雙 ORM 指標不一致時，精準標記 dataConsistent 為 false")
    void testDualOrmInconsistencyDetection() {
        ReportFilterRequest request = ReportFilterRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        ReportSummaryDto mbSummary = ReportSummaryDto.builder()
                .totalAnalyzedCount(4L)
                .selfCheckoutUsageRate(new BigDecimal("50.00"))
                .build();

        ReportSummaryDto jpaSummary = ReportSummaryDto.builder()
                .totalAnalyzedCount(5L) // 資料不一致
                .selfCheckoutUsageRate(new BigDecimal("60.00"))
                .build();

        when(myBatisAnalyticsService.calculateSummary(any())).thenReturn(mbSummary);
        when(jpaAnalyticsService.calculateSummary(any())).thenReturn(jpaSummary);

        OrmComparisonResponse response = analyticsService.getComparedAnalytics(request);

        assertThat(response).isNotNull();
        assertThat(response.getDataConsistent()).isFalse();
    }
}
