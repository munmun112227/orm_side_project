package com.pos.analytics.service;

import com.pos.analytics.dto.*;
import com.pos.analytics.repository.TransactionJpaRepository;
import com.pos.analytics.util.HolidayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基於 Spring Data JPA 實作的營運指標計算服務。
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class JpaAnalyticsService {

    private final TransactionJpaRepository repository;

    @Transactional(readOnly = true)
    public ReportSummaryDto calculateSummary(ReportFilterRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        log.info("開始執行 Spring Data JPA 報表指標計算，區間: {} ~ {}",
                HolidayUtils.sanitizeForLog(startDate), HolidayUtils.sanitizeForLog(endDate));

        // 1. 總筆數與支付加總
        Map<String, Object> overview = repository.queryTerminalOverview(startDate, endDate);
        long cashierCount = getLongValue(overview, "cashier_count");
        long selfCheckoutCount = getLongValue(overview, "self_checkout_count");
        BigDecimal cashierPayment = getBigDecimalValue(overview, "cashier_total_payment");
        BigDecimal selfCheckoutPayment = getBigDecimalValue(overview, "self_checkout_total_payment");

        long totalCount = cashierCount + selfCheckoutCount;
        BigDecimal totalPayment = cashierPayment.add(selfCheckoutPayment);

        // 自助結帳機使用率
        BigDecimal usageRate = BigDecimal.ZERO;
        if (totalCount > 0) {
            usageRate = BigDecimal.valueOf(selfCheckoutCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
        }

        // 客單價 (以 payment 計算)
        BigDecimal cashierAvgTicket = cashierCount > 0
                ? cashierPayment.divide(BigDecimal.valueOf(cashierCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal selfCheckoutAvgTicket = selfCheckoutCount > 0
                ? selfCheckoutPayment.divide(BigDecimal.valueOf(selfCheckoutCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal overallAvgTicket = totalCount > 0
                ? totalPayment.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. 結帳速度 (假日 13:30~17:30)
        int holidayDays = HolidayUtils.countWeekendDays(startDate, endDate);
        int halfHourSlots = Math.max(1, holidayDays * 8);

        List<Map<String, Object>> cashierStaffRows = repository.queryCashierStaffHolidayTx(startDate, endDate);
        List<CheckoutSpeedDto> cashierStaffSpeeds = new ArrayList<>();
        for (Map<String, Object> row : cashierStaffRows) {
            String identifier = String.valueOf(row.get("identifier"));
            long tx = getLongValue(row, "total_tx");
            BigDecimal avg = BigDecimal.valueOf(tx).divide(BigDecimal.valueOf(halfHourSlots), 2, RoundingMode.HALF_UP);
            cashierStaffSpeeds.add(CheckoutSpeedDto.builder()
                    .terminalType("CASHIER")
                    .identifier(identifier)
                    .totalTransactions(tx)
                    .avgTransactionsPerHalfHour(avg)
                    .build());
        }

        List<Map<String, Object>> scoTerminalRows = repository.querySelfCheckoutTerminalHolidayTx(startDate, endDate);
        List<CheckoutSpeedDto> scoTerminalSpeeds = new ArrayList<>();
        for (Map<String, Object> row : scoTerminalRows) {
            String identifier = String.valueOf(row.get("identifier"));
            long tx = getLongValue(row, "total_tx");
            BigDecimal avg = BigDecimal.valueOf(tx).divide(BigDecimal.valueOf(halfHourSlots), 2, RoundingMode.HALF_UP);
            scoTerminalSpeeds.add(CheckoutSpeedDto.builder()
                    .terminalType("SELF_CHECKOUT")
                    .identifier(identifier)
                    .totalTransactions(tx)
                    .avgTransactionsPerHalfHour(avg)
                    .build());
        }

        // 3. 15 分鐘時段分佈
        List<Map<String, Object>> timeRows = repository.query15MinuteDistribution(startDate, endDate);
        List<TimeDistributionDto> timeDistributions = new ArrayList<>();
        for (Map<String, Object> row : timeRows) {
            timeDistributions.add(TimeDistributionDto.builder()
                    .timeSlot(String.valueOf(row.get("slot_start")))
                    .cashierCount(getLongValue(row, "cashier_count"))
                    .selfCheckoutCount(getLongValue(row, "self_checkout_count"))
                    .totalCount(getLongValue(row, "total_count"))
                    .build());
        }

        // 4. 付款別分析
        List<Map<String, Object>> tenderRows = repository.queryTenderTypeDistribution(startDate, endDate);
        List<TenderTypeDto> tenderDistributions = new ArrayList<>();
        for (Map<String, Object> row : tenderRows) {
            long txCount = getLongValue(row, "tx_count");
            BigDecimal amt = getBigDecimalValue(row, "total_amount");
            BigDecimal pct = totalCount > 0
                    ? BigDecimal.valueOf(txCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            tenderDistributions.add(TenderTypeDto.builder()
                    .tenderType(String.valueOf(row.get("tender_type")))
                    .transactionCount(txCount)
                    .totalAmount(amt)
                    .percentage(pct)
                    .build());
        }

        return ReportSummaryDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .cashierTransactionCount(cashierCount)
                .selfCheckoutTransactionCount(selfCheckoutCount)
                .totalAnalyzedCount(totalCount)
                .selfCheckoutUsageRate(usageRate)
                .holidayDaysCount(holidayDays)
                .cashierStaffSpeeds(cashierStaffSpeeds)
                .selfCheckoutTerminalSpeeds(scoTerminalSpeeds)
                .timeDistributions(timeDistributions)
                .tenderTypeDistributions(tenderDistributions)
                .cashierTotalPayment(cashierPayment)
                .selfCheckoutTotalPayment(selfCheckoutPayment)
                .totalPayment(totalPayment)
                .cashierAverageTicket(cashierAvgTicket)
                .selfCheckoutAverageTicket(selfCheckoutAvgTicket)
                .overallAverageTicket(overallAvgTicket)
                .build();
    }

    private long getLongValue(Map<String, Object> map, String key) {
        if (map == null) return 0L;
        Object val = map.get(key);
        if (val == null) {
            val = map.get(key.toUpperCase());
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        if (map == null) return BigDecimal.ZERO;
        Object val = map.get(key);
        if (val == null) {
            val = map.get(key.toUpperCase());
        }
        if (val instanceof BigDecimal bd) {
            return bd;
        } else if (val instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
