package com.pos.analytics.service;

import com.pos.analytics.dto.CheckoutSpeedDto;
import com.pos.analytics.dto.ReportSummaryDto;
import com.pos.analytics.dto.TenderTypeDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PdfReportExportService 報表生成測試 (PDFBox 3.x)")
class PdfReportExportServiceTest {

    private final PdfReportExportService pdfService = new PdfReportExportService();

    @Test
    @DisplayName("驗證 PDF 報表生成與文件合法性")
    void testGeneratePdfReport() throws IOException {
        ReportSummaryDto mockSummary = ReportSummaryDto.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .cashierTransactionCount(3000L)
                .selfCheckoutTransactionCount(2000L)
                .totalAnalyzedCount(5000L)
                .selfCheckoutUsageRate(new BigDecimal("40.00"))
                .holidayDaysCount(8)
                .cashierStaffSpeeds(List.of(
                        CheckoutSpeedDto.builder()
                                .terminalType("CASHIER")
                                .identifier("EMP001")
                                .totalTransactions(320L)
                                .avgTransactionsPerHalfHour(new BigDecimal("5.00"))
                                .build()
                ))
                .selfCheckoutTerminalSpeeds(List.of(
                        CheckoutSpeedDto.builder()
                                .terminalType("SELF_CHECKOUT")
                                .identifier("QPSCE01")
                                .totalTransactions(400L)
                                .avgTransactionsPerHalfHour(new BigDecimal("6.25"))
                                .build()
                ))
                .tenderTypeDistributions(List.of(
                        TenderTypeDto.builder()
                                .tenderType("Credit Card")
                                .transactionCount(2500L)
                                .totalAmount(new BigDecimal("150000.00"))
                                .percentage(new BigDecimal("50.00"))
                                .build()
                ))
                .cashierTotalPayment(new BigDecimal("180000.00"))
                .selfCheckoutTotalPayment(new BigDecimal("120000.00"))
                .totalPayment(new BigDecimal("300000.00"))
                .cashierAverageTicket(new BigDecimal("60.00"))
                .selfCheckoutAverageTicket(new BigDecimal("60.00"))
                .overallAverageTicket(new BigDecimal("60.00"))
                .build();

        byte[] pdfBytes = pdfService.generatePdfReport(mockSummary, 45L, 55L);

        assertThat(pdfBytes).isNotNull().isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(500);

        // 使用 PDFBox Loader 驗證該 byte[] 確實為合法的 PDF 文件
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }
}
