package com.pos.analytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 報表查詢過濾條件 DTO。
 * 使用嚴格的 LocalDate 型別防止 SQL 注入與非法格式字串輸入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {

    /**
     * 查詢起始日期 (含)
     */
    @NotNull(message = "起始日期不得為空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /**
     * 查詢結束日期 (含)
     */
    @NotNull(message = "結束日期不得為空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
