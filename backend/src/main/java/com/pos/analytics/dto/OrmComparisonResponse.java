package com.pos.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 雙 ORM 效能對比回應物件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrmComparisonResponse {

    /**
     * 報表計算指標資料
     */
    private ReportSummaryDto data;

    /**
     * MyBatis 執行耗時 (毫秒)
     */
    private Long myBatisDurationMs;

    /**
     * Spring Data JPA 執行耗時 (毫秒)
     */
    private Long jpaDurationMs;

    /**
     * 表現較佳之 ORM 名稱
     */
    private String fasterOrm;

    /**
     * 效能差距倍數或百分比說明
     */
    private String speedComparisonSummary;

    /**
     * 兩者計算結果一致性檢驗結果 (TRUE: 驗證通過)
     */
    private Boolean dataConsistent;
}
