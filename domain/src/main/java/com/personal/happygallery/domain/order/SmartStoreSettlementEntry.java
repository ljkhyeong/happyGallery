package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "smartstore_settlement_entries")
public class SmartStoreSettlementEntry {

    @Id
    @Column(name = "entry_key", length = 255)
    private String entryKey;

    @Column(name = "product_order_id", length = 30)
    private String productOrderId;

    @Column(name = "order_id", length = 30)
    private String orderId;

    @Column(name = "product_order_type", nullable = false, length = 50)
    private String productOrderType;

    @Column(name = "settle_type", length = 50)
    private String settleType;

    @Column(name = "product_name", length = 4000)
    private String productName;

    @Column(name = "pay_settle_amount", nullable = false)
    private long paySettleAmount;

    @Column(name = "total_pay_commission_amount")
    private Long totalPayCommissionAmount;

    @Column(name = "selling_interlock_commission_amount")
    private Long sellingInterlockCommissionAmount;

    @Column(name = "benefit_settle_amount", nullable = false)
    private long benefitSettleAmount;

    @Column(name = "settle_expect_amount", nullable = false)
    private long settleExpectAmount;

    @Column(name = "settle_basis_date")
    private LocalDate settleBasisDate;

    @Column(name = "settle_expect_date")
    private LocalDate settleExpectDate;

    @Column(name = "settle_complete_date")
    private LocalDate settleCompleteDate;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private SmartStoreSettlementStatus reconciliationStatus;

    @Column(name = "reconciliation_reason", length = 500)
    private String reconciliationReason;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected SmartStoreSettlementEntry() {}

    public static SmartStoreSettlementEntry create(String entryKey) {
        SmartStoreSettlementEntry entry = new SmartStoreSettlementEntry();
        entry.entryKey = entryKey;
        return entry;
    }

    public void synchronize(
            String productOrderId,
            String orderId,
            String productOrderType,
            String settleType,
            String productName,
            long paySettleAmount,
            Long totalPayCommissionAmount,
            Long sellingInterlockCommissionAmount,
            long benefitSettleAmount,
            long settleExpectAmount,
            LocalDate settleBasisDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            LocalDate payDate,
            SmartStoreSettlementStatus reconciliationStatus,
            String reconciliationReason,
            LocalDateTime fetchedAt) {
        this.productOrderId = productOrderId;
        this.orderId = orderId;
        this.productOrderType = productOrderType;
        this.settleType = settleType;
        this.productName = productName;
        this.paySettleAmount = paySettleAmount;
        this.totalPayCommissionAmount = totalPayCommissionAmount;
        this.sellingInterlockCommissionAmount = sellingInterlockCommissionAmount;
        this.benefitSettleAmount = benefitSettleAmount;
        this.settleExpectAmount = settleExpectAmount;
        this.settleBasisDate = settleBasisDate;
        this.settleExpectDate = settleExpectDate;
        this.settleCompleteDate = settleCompleteDate;
        this.payDate = payDate;
        this.reconciliationStatus = reconciliationStatus;
        this.reconciliationReason = reconciliationReason;
        this.fetchedAt = fetchedAt;
    }

    public String getEntryKey() { return entryKey; }
    public String getProductOrderId() { return productOrderId; }
    public String getOrderId() { return orderId; }
    public String getProductOrderType() { return productOrderType; }
    public String getSettleType() { return settleType; }
    public String getProductName() { return productName; }
    public long getPaySettleAmount() { return paySettleAmount; }
    public Long getTotalPayCommissionAmount() { return totalPayCommissionAmount; }
    public Long getSellingInterlockCommissionAmount() { return sellingInterlockCommissionAmount; }
    public long getBenefitSettleAmount() { return benefitSettleAmount; }
    public long getSettleExpectAmount() { return settleExpectAmount; }
    public LocalDate getSettleBasisDate() { return settleBasisDate; }
    public LocalDate getSettleExpectDate() { return settleExpectDate; }
    public LocalDate getSettleCompleteDate() { return settleCompleteDate; }
    public LocalDate getPayDate() { return payDate; }
    public SmartStoreSettlementStatus getReconciliationStatus() { return reconciliationStatus; }
    public String getReconciliationReason() { return reconciliationReason; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
}
