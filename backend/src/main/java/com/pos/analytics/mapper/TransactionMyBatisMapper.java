package com.pos.analytics.mapper;

import com.pos.analytics.dto.CheckoutSpeedDto;
import com.pos.analytics.dto.TenderTypeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * MyBatis 交易數據分析 Mapper 介面。
 * 遵循 Checkmarx 安全準則：嚴格使用預編譯參數，杜絕 SQL 注入。
 */
@Mapper
public interface TransactionMyBatisMapper {

    /**
     * 查詢 QPSCL 與 QPSCE 之總筆數與支付加總。
     */
    Map<String, Object> queryTerminalOverview(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查詢假日 13:30~17:30 人工機各 staff_id 總交易筆數。
     */
    List<Map<String, Object>> queryCashierStaffHolidayTx(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查詢假日 13:30~17:30 自助機各 pos_terminal_no 總交易筆數。
     */
    List<Map<String, Object>> querySelfCheckoutTerminalHolidayTx(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查詢每 15 分鐘時段交易筆數分佈。
     */
    List<Map<String, Object>> query15MinuteDistribution(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查詢各支付方式 (tender_type) 之交易筆數與總金額。
     */
    List<Map<String, Object>> queryTenderTypeDistribution(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
