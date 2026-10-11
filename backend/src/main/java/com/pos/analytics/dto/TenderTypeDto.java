package com.pos.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付別分析 (Tender Type) DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderTypeDto {

    /**
     * 支付工具名稱，如 "Credit Card-Self Checkout", "LINE Pay"
     */
    private String tenderType;

    /**
     * 交易筆數
     */
    private Long transactionCount;

    /**
     * 總金額 (依 payment 欄位加總)
     */
    private BigDecimal totalAmount;

    /**
     * 佔總交易筆數百分比 (0~100)
     */
    private BigDecimal percentage;
}
