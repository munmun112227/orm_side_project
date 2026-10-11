package com.pos.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * POS 交易紀錄 JPA 實體。
 * 對應資料庫 transactions 表。
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_date", columnList = "trans_date"),
        @Index(name = "idx_tx_pos_terminal", columnList = "pos_terminal_no")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_month", nullable = false)
    private Short sourceMonth;

    @Column(name = "transaction_no", length = 50)
    private String transactionNo;

    @Column(name = "gui_no", length = 50)
    private String guiNo;

    @Column(name = "carrier_code", length = 100)
    private String carrierCode;

    @Column(name = "donation_code", length = 50)
    private String donationCode;

    @Column(name = "ib_member", length = 50)
    private String ibMember;

    @Column(name = "ikea_member", length = 50)
    private String ikeaMember;

    @Column(name = "cui_customer", length = 50)
    private String cuiCustomer;

    @Column(name = "tax_exemption_customer", length = 50)
    private String taxExemptionCustomer;

    @Column(name = "tender_type", length = 100)
    private String tenderType;

    @Column(name = "card_number", length = 50)
    private String cardNumber;

    @Column(name = "receipt_no", length = 100)
    private String receiptNo;

    @Column(name = "original_receipt_no", length = 100)
    private String originalReceiptNo;

    @Column(name = "order_no", length = 100)
    private String orderNo;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    @Column(name = "store_no", length = 50)
    private String storeNo;

    @Column(name = "pos_terminal_no", length = 50)
    private String posTerminalNo;

    @Column(name = "staff_id", length = 50)
    private String staffId;

    @Column(name = "trans_date")
    private LocalDate transDate;

    @Column(name = "trans_time")
    private LocalTime transTime;

    @Column(name = "customer_no", length = 50)
    private String customerNo;

    @Column(name = "sales_type", length = 50)
    private String salesType;

    @Column(name = "net_amount", precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "gross_amount", precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "payment", precision = 14, scale = 2)
    private BigDecimal payment;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "cost_amount", precision = 14, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "training")
    private Boolean training;

    @Column(name = "statement_no", length = 50)
    private String statementNo;

    @Column(name = "posting_status", length = 50)
    private String postingStatus;

    @Column(name = "trans_gui_no_exists")
    private Boolean transGuiNoExists;
}
