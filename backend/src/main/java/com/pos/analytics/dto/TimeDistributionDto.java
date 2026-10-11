package com.pos.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每 15 分鐘時段交易筆數分析 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeDistributionDto {

    /**
     * 時段標籤，例如 "13:30-13:44"
     */
    private String timeSlot;

    /**
     * 人工結帳機 (QPSCL) 交易筆數
     */
    private Long cashierCount;

    /**
     * 自助結帳機 (QPSCE) 交易筆數
     */
    private Long selfCheckoutCount;

    /**
     * 全體交易筆數
     */
    private Long totalCount;
}
