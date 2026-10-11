package com.pos.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 假日 13:30~17:30 結帳速度 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSpeedDto {

    /**
     * 機台類型：CASHIER (人工機) 或 SELF_CHECKOUT (自助機)
     */
    private String terminalType;

    /**
     * 人工機為員工編號 staff_id；自助機為機台編號 pos_terminal_no
     */
    private String identifier;

    /**
     * 統計區間內總交易筆數
     */
    private Long totalTransactions;

    /**
     * 每半小時平均交易筆數 (筆/0.5hr)
     */
    private BigDecimal avgTransactionsPerHalfHour;
}
