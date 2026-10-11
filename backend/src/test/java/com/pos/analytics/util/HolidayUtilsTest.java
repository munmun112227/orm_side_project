package com.pos.analytics.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HolidayUtils 單元測試")
class HolidayUtilsTest {

    @Test
    @DisplayName("測試計算區間內週六日天數")
    void testCountWeekendDays() {
        // 2024-01-01 (Mon) 到 2024-01-07 (Sun)：包含 01-06 (Sat) 與 01-07 (Sun)，共 2 天
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 7);

        int count = HolidayUtils.countWeekendDays(start, end);
        assertThat(count).isEqualTo(2);

        // 測試跨一個完整月份 (2024年1月有 31 天，包含 4 個週六與 4 個週日 = 8 天)
        int janCount = HolidayUtils.countWeekendDays(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        // 2024-01-06, 07, 13, 14, 20, 21, 27, 28 -> 8 天
        assertThat(janCount).isEqualTo(8);

        // 邊界條件：起日晚於迄日
        assertThat(HolidayUtils.countWeekendDays(end, start)).isZero();
        assertThat(HolidayUtils.countWeekendDays(null, end)).isZero();
    }

    @Test
    @DisplayName("測試日誌換行字元消毒防止 CRLF 注入")
    void testSanitizeForLog() {
        String dirtyInput = "2024-01-01\r\nADMIN_LOGIN_SUCCESS";
        String clean = HolidayUtils.sanitizeForLog(dirtyInput);
        assertThat(clean).doesNotContain("\r").doesNotContain("\n");
        assertThat(clean).isEqualTo("2024-01-01__ADMIN_LOGIN_SUCCESS");
    }
}
