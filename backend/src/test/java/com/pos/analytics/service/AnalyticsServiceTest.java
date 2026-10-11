package com.pos.analytics.service;

import com.pos.analytics.dto.OrmComparisonResponse;
import com.pos.analytics.dto.ReportFilterRequest;
import com.pos.analytics.dto.ReportSummaryDto;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService 雙 ORM 接續執行與效能比對測試")
class AnalyticsServiceTest {

    @Mock
    private MyBatisAnalyticsService myBatisService;

    @Mock
    private JpaAnalyticsService jpaService;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("驗證雙 ORM 接續呼叫與一致性回傳")
    void testGetComparedAnalytics() {
        ReportFilterRequest request = ReportFilterRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        ReportSummaryDto sampleSummary = ReportSummaryDto.builder()
                .cashierTransactionCount(100L)
                .selfCheckoutTransactionCount(50L)
                .totalAnalyzedCount(150L)
                .selfCheckoutUsageRate(new BigDecimal("33.33"))
                .cashierAverageTicket(new BigDecimal("120.00"))
                .selfCheckoutAverageTicket(new BigDecimal("110.00"))
                .overallAverageTicket(new BigDecimal("116.67"))
                .build();

        when(myBatisService.calculateSummary(any())).thenReturn(sampleSummary);
        when(jpaService.calculateSummary(any())).thenReturn(sampleSummary);

        OrmComparisonResponse response = analyticsService.getComparedAnalytics(request);

        // 驗證兩者皆被依序呼叫
        verify(myBatisService, times(1)).calculateSummary(request);
        verify(jpaService, times(1)).calculateSummary(request);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getMyBatisDurationMs()).isNotNull();
        assertThat(response.getJpaDurationMs()).isNotNull();
        assertThat(response.getDataConsistent()).isTrue();
        assertThat(response.getFasterOrm()).isIn("MyBatis", "Spring Data JPA", "平手 (Tie)");
    }
}
