package com.pos.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 完整營運分析指標匯總 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {

    /**
     * 統計區間
     */
    private LocalDate startDate;
    private LocalDate endDate;

    // ==========================================
    // 1. 自助結帳機使用率
    // ==========================================
    private Long cashierTransactionCount;       // 人工機 (QPSCL) 總筆數
    private Long selfCheckoutTransactionCount;  // 自助機 (QPSCE) 總筆數
    private Long totalAnalyzedCount;            // 分析總筆數 (人工+自助)
    private BigDecimal selfCheckoutUsageRate;   // 自助結帳使用率 (%)

    // ==========================================
    // 2. 結帳速度 (假日 13:30~17:30)
    // ==========================================
    private Integer holidayDaysCount;           // 區間內假日天數
    private List<CheckoutSpeedDto> cashierStaffSpeeds;       // 人工機個別員工平均速度
    private List<CheckoutSpeedDto> selfCheckoutTerminalSpeeds; // 自助機個別機台平均速度

    // ==========================================
    // 3. 時段分析 (每 15 分鐘)
    // ==========================================
    private List<TimeDistributionDto> timeDistributions;

    // ==========================================
    // 4. 付款別分析 (Tender Type)
    // ==========================================
    private List<TenderTypeDto> tenderTypeDistributions;

    // ==========================================
    // 5. 客單價分析 (依 payment 計算)
    // ==========================================
    private BigDecimal cashierTotalPayment;       // 人工機總支付金額
    private BigDecimal selfCheckoutTotalPayment;  // 自助機總支付金額
    private BigDecimal totalPayment;              // 全體總支付金額

    private BigDecimal cashierAverageTicket;      // 數字 1：人工機客單價
    private BigDecimal selfCheckoutAverageTicket; // 數字 2：自助機客單價
    private BigDecimal overallAverageTicket;      // 數字 3：全體整合客單價
}
