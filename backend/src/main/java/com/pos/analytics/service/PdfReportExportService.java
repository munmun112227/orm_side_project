package com.pos.analytics.service;

import com.pos.analytics.dto.CheckoutSpeedDto;
import com.pos.analytics.dto.ReportSummaryDto;
import com.pos.analytics.dto.TenderTypeDto;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 基於 Apache PDFBox 3.x 實作的 PDF 營運報表生成服務。
 * 遵循安全編碼標準，使用 try-with-resources 杜絕資源流失與內存洩漏。
 */
@Service
@Log4j2
public class PdfReportExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成營運報表 PDF 二進制數據。
     *
     * @param summary 報表數據匯總
     * @param myBatisDurationMs MyBatis 執行耗時
     * @param jpaDurationMs JPA 執行耗時
     * @return PDF byte 陣列
     */
    public byte[] generatePdfReport(ReportSummaryDto summary, Long myBatisDurationMs, Long jpaDurationMs) throws IOException {
        log.info("開始生成 PDF 營運分析報表 (PDFBox 3.x)...");

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // 第 1 頁：總覽與核心 KPI
            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            try (PDPageContentStream cs = new PDPageContentStream(document, page1)) {
                float y = 790;

                // 標題與裝飾線
                drawText(cs, fontBold, 18, 50, y, "POS Self-Checkout & Performance Analytics Report");
                y -= 20;
                drawText(cs, fontRegular, 10, 50, y, "Generated At: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
                y -= 15;
                drawText(cs, fontRegular, 10, 50, y, String.format("Query Date Range: %s ~ %s", summary.getStartDate(), summary.getEndDate()));
                y -= 15;

                // 分隔線
                cs.setLineWidth(1.2f);
                cs.moveTo(50, y);
                cs.lineTo(545, y);
                cs.stroke();
                y -= 25;

                // 區塊 1: ORM Benchmark 效能對比
                drawText(cs, fontBold, 13, 50, y, "1. Dual ORM Benchmark (Execution Time)");
                y -= 18;
                drawText(cs, fontRegular, 10, 65, y, String.format("- MyBatis Duration: %d ms", myBatisDurationMs));
                y -= 15;
                drawText(cs, fontRegular, 10, 65, y, String.format("- Spring Data JPA Duration: %d ms", jpaDurationMs));
                y -= 15;
                String winner = myBatisDurationMs <= jpaDurationMs ? "MyBatis" : "Spring Data JPA";
                drawText(cs, fontBold, 10, 65, y, String.format("- Faster ORM: %s (Execution gap: %d ms)", winner, Math.abs(jpaDurationMs - myBatisDurationMs)));
                y -= 25;

                // 區塊 2: 核心營運指標 (五大指標摘要)
                drawText(cs, fontBold, 13, 50, y, "2. Key Operation Metrics & Average Ticket (Payment)");
                y -= 18;
                drawText(cs, fontRegular, 10, 65, y, String.format("- SCO Usage Rate: %s %% (SCO: %d, Cashier: %d, Total: %d)",
                        summary.getSelfCheckoutUsageRate(),
                        summary.getSelfCheckoutTransactionCount(),
                        summary.getCashierTransactionCount(),
                        summary.getTotalAnalyzedCount()));
                y -= 15;
                drawText(cs, fontRegular, 10, 65, y, String.format("- Cashier POS Avg Ticket (QPSCL01~20): $ %s", summary.getCashierAverageTicket()));
                y -= 15;
                drawText(cs, fontRegular, 10, 65, y, String.format("- Self-Checkout Avg Ticket (QPSCE01~20): $ %s", summary.getSelfCheckoutAverageTicket()));
                y -= 15;
                drawText(cs, fontRegular, 10, 65, y, String.format("- Overall Average Ticket: $ %s", summary.getOverallAverageTicket()));
                y -= 25;

                // 區塊 3: 假日高峰速度分析 (13:30~17:30)
                drawText(cs, fontBold, 13, 50, y, String.format("3. Weekend Peak Checkout Speed (13:30-17:30, %d Weekend Days)", summary.getHolidayDaysCount()));
                y -= 18;

                drawText(cs, fontBold, 10, 65, y, "Top Staff Speeds (Cashier / QPSCL):");
                y -= 14;
                List<CheckoutSpeedDto> cashierSpeeds = summary.getCashierStaffSpeeds();
                int limit = Math.min(6, cashierSpeeds.size());
                for (int i = 0; i < limit; i++) {
                    CheckoutSpeedDto s = cashierSpeeds.get(i);
                    drawText(cs, fontRegular, 9, 75, y, String.format("%d. Staff ID: %-12s | Total Tx: %-5d | Avg/0.5hr: %s",
                            (i + 1), s.getIdentifier(), s.getTotalTransactions(), s.getAvgTransactionsPerHalfHour()));
                    y -= 13;
                }
                if (limit == 0) {
                    drawText(cs, fontRegular, 9, 75, y, "(No cashier transactions in weekend peak window)");
                    y -= 13;
                }

                y -= 10;
                drawText(cs, fontBold, 10, 65, y, "Self-Checkout Terminal Speeds (QPSCE):");
                y -= 14;
                List<CheckoutSpeedDto> scoSpeeds = summary.getSelfCheckoutTerminalSpeeds();
                int scoLimit = Math.min(6, scoSpeeds.size());
                for (int i = 0; i < scoLimit; i++) {
                    CheckoutSpeedDto s = scoSpeeds.get(i);
                    drawText(cs, fontRegular, 9, 75, y, String.format("%d. Terminal: %-12s | Total Tx: %-5d | Avg/0.5hr: %s",
                            (i + 1), s.getIdentifier(), s.getTotalTransactions(), s.getAvgTransactionsPerHalfHour()));
                    y -= 13;
                }
                if (scoLimit == 0) {
                    drawText(cs, fontRegular, 9, 75, y, "(No self-checkout transactions in weekend peak window)");
                    y -= 13;
                }

                y -= 20;

                // 區塊 4: 支付工具別 (Tender Type)
                drawText(cs, fontBold, 13, 50, y, "4. Tender Type Payment Distribution");
                y -= 18;
                drawText(cs, fontBold, 9, 65, y, String.format("%-30s | %-12s | %-14s | %s", "Tender Type", "Tx Count", "Total Payment", "Percentage"));
                y -= 12;

                List<TenderTypeDto> tenders = summary.getTenderTypeDistributions();
                int tenderLimit = Math.min(8, tenders.size());
                for (int i = 0; i < tenderLimit; i++) {
                    TenderTypeDto t = tenders.get(i);
                    String name = t.getTenderType().length() > 28 ? t.getTenderType().substring(0, 28) : t.getTenderType();
                    drawText(cs, fontRegular, 9, 65, y, String.format("%-30s | %-12d | $ %-12s | %s %%",
                            name, t.getTransactionCount(), t.getTotalAmount(), t.getPercentage()));
                    y -= 13;
                }

                // 頁尾
                drawText(cs, fontRegular, 8, 250, 30, "Page 1 of 1 - Confidential Internal Report");
            }

            document.save(baos);
            log.info("PDF 報表產製完成，大小: {} bytes", baos.size());
            return baos.toByteArray();
        }
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float fontSize, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        // 清理非 ASCII 字元，避免 Standard14 字型丟出例外
        String safeText = text.replaceAll("[^\\x00-\\x7F]", "?");
        cs.showText(safeText);
        cs.endText();
    }
}
