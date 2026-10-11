package com.pos.analytics.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 假日與安全處理工具類別。
 */
public final class HolidayUtils {

    private HolidayUtils() {
        // 私有建構子，防止實例化
    }

    /**
     * 計算指定閉區間 [startDate, endDate] 內的週六與週日總天數。
     *
     * @param startDate 起始日期
     * @param endDate 結束日期
     * @return 假日天數 (最少為 0)
     */
    public static int countWeekendDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }

        int count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    /**
     * 清理字串中的 CRLF 換行字元，防止 Log Injection (CWE-117)。
     *
     * @param input 原始字串
     * @return 消毒後的字串
     */
    public static String sanitizeForLog(Object input) {
        if (input == null) {
            return "null";
        }
        return input.toString().replaceAll("[\r\n]", "_");
    }
}
