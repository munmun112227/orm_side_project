package com.pos.analytics.repository;

import com.pos.analytics.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Spring Data JPA 交易資料倉儲介面。
 * 包含原生/JPQL 聚合查詢，與 MyBatis 實現相同指標計算，便於進行精準效能比對。
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Long> {

    /**
     * 1. 查詢 QPSCL 與 QPSCE 之總筆數與支付金額總計
     */
    @Query(value = """
            SELECT
                COUNT(CASE WHEN pos_terminal_no >= 'QPSCL01' AND pos_terminal_no <= 'QPSCL20' THEN 1 END) AS cashier_count,
                COUNT(CASE WHEN pos_terminal_no >= 'QPSCE01' AND pos_terminal_no <= 'QPSCE20' THEN 1 END) AS self_checkout_count,
                COALESCE(SUM(CASE WHEN pos_terminal_no >= 'QPSCL01' AND pos_terminal_no <= 'QPSCL20' THEN payment ELSE 0 END), 0) AS cashier_total_payment,
                COALESCE(SUM(CASE WHEN pos_terminal_no >= 'QPSCE01' AND pos_terminal_no <= 'QPSCE20' THEN payment ELSE 0 END), 0) AS self_checkout_total_payment
            FROM transactions
            WHERE trans_date >= :startDate
              AND trans_date <= :endDate
              AND (
                    (pos_terminal_no >= 'QPSCL01' AND pos_terminal_no <= 'QPSCL20')
                 OR (pos_terminal_no >= 'QPSCE01' AND pos_terminal_no <= 'QPSCE20')
              )
              AND (training IS NOT TRUE)
            """, nativeQuery = true)
    Map<String, Object> queryTerminalOverview(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 2. 查詢假日 13:30~17:30 人工機個別員工總交易筆數
     */
    @Query(value = """
            SELECT
                COALESCE(staff_id, 'UNKNOWN') AS identifier,
                COUNT(*) AS total_tx
            FROM transactions
            WHERE trans_date >= :startDate
              AND trans_date <= :endDate
              AND EXTRACT(ISODOW FROM trans_date) IN (6, 7)
              AND trans_time >= TIME '13:30:00'
              AND trans_time <= TIME '17:30:00'
              AND pos_terminal_no >= 'QPSCL01'
              AND pos_terminal_no <= 'QPSCL20'
              AND (training IS NOT TRUE)
            GROUP BY staff_id
            ORDER BY total_tx DESC
            """, nativeQuery = true)
    List<Map<String, Object>> queryCashierStaffHolidayTx(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 3. 查詢假日 13:30~17:30 自助機個別機台總交易筆數
     */
    @Query(value = """
            SELECT
                pos_terminal_no AS identifier,
                COUNT(*) AS total_tx
            FROM transactions
            WHERE trans_date >= :startDate
              AND trans_date <= :endDate
              AND EXTRACT(ISODOW FROM trans_date) IN (6, 7)
              AND trans_time >= TIME '13:30:00'
              AND trans_time <= TIME '17:30:00'
              AND pos_terminal_no >= 'QPSCE01'
              AND pos_terminal_no <= 'QPSCE20'
              AND (training IS NOT TRUE)
            GROUP BY pos_terminal_no
            ORDER BY pos_terminal_no ASC
            """, nativeQuery = true)
    List<Map<String, Object>> querySelfCheckoutTerminalHolidayTx(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 4. 查詢每 15 分鐘時段交易筆數分佈
     */
    @Query(value = """
            SELECT
                TO_CHAR(
                    TIME '00:00:00' + (FLOOR(EXTRACT(HOUR FROM trans_time) * 4 + EXTRACT(MINUTE FROM trans_time) / 15) * INTERVAL '15 minute'),
                    'HH24:MI'
                ) AS slot_start,
                COUNT(CASE WHEN pos_terminal_no >= 'QPSCL01' AND pos_terminal_no <= 'QPSCL20' THEN 1 END) AS cashier_count,
                COUNT(CASE WHEN pos_terminal_no >= 'QPSCE01' AND pos_terminal_no <= 'QPSCE20' THEN 1 END) AS self_checkout_count,
                COUNT(*) AS total_count
            FROM transactions
            WHERE trans_date >= :startDate
              AND trans_date <= :endDate
              AND trans_time IS NOT NULL
              AND (
                    (pos_terminal_no >= 'QPSCL01' AND pos_terminal_no <= 'QPSCL20')
                 OR (pos_terminal_no >= 'QPSCE01' AND pos_terminal_no <= 'QPSCE20')
              )
              AND (training IS NOT TRUE)
            GROUP BY slot_start
            ORDER BY slot_start ASC
            """, nativeQuery = true)
    List<Map<String, Object>> query15MinuteDistribution(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 5. 查詢各支付方式 (tender_type) 之交易筆數與總金額
     */
    @Query(value = """
            SELECT
                COALESCE(tender_type, 'UNKNOWN') AS tender_type,
                COUNT(*) AS tx_count,
                COALESCE(SUM(payment), 0) AS total_amount
            FROM transactions
            WHERE trans_date >= :startDate
              AND trans_date <= :endDate
              AND (
                    (pos_terminal_no >= 'QPSCL01' AND pos_terminal_no <= 'QPSCL20')
                 OR (pos_terminal_no >= 'QPSCE01' AND pos_terminal_no <= 'QPSCE20')
              )
              AND (training IS NOT TRUE)
            GROUP BY tender_type
            ORDER BY tx_count DESC
            """, nativeQuery = true)
    List<Map<String, Object>> queryTenderTypeDistribution(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
