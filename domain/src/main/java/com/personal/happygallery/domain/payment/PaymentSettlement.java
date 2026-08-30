package com.personal.happygallery.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_settlements")
public class PaymentSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_key", nullable = false, unique = true, length = 64)
    private String transactionKey;

    @Column(name = "payment_key", nullable = false, length = 200)
    private String paymentKey;

    @Column(name = "order_id_external", length = 64)
    private String orderIdExternal;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(nullable = false)
    private long amount;

    @Column(name = "fee_amount", nullable = false)
    private long feeAmount;

    @Column(name = "supply_amount", nullable = false)
    private long supplyAmount;

    @Column(nullable = false)
    private long vat;

    @Column(name = "pay_out_amount", nullable = false)
    private long payOutAmount;

    @Column(name = "approved_at", length = 40)
    private String approvedAt;

    @Column(name = "sold_date", nullable = false)
    private LocalDate soldDate;

    @Column(name = "paid_out_date")
    private LocalDate paidOutDate;

    @Column(name = "cancel_transaction", nullable = false)
    private boolean cancelTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private PaymentSettlementStatus reconciliationStatus;

    @Column(name = "reconciliation_reason", length = 500)
    private String reconciliationReason;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected PaymentSettlement() {}

    public static PaymentSettlement create(String transactionKey) {
        PaymentSettlement settlement = new PaymentSettlement();
        settlement.transactionKey = transactionKey;
        return settlement;
    }

    public void synchronize(
            String paymentKey,
            String orderIdExternal,
            String paymentMethod,
            long amount,
            long feeAmount,
            long supplyAmount,
            long vat,
            long payOutAmount,
            String approvedAt,
            LocalDate soldDate,
            LocalDate paidOutDate,
            boolean cancelTransaction,
            PaymentSettlementStatus reconciliationStatus,
            String reconciliationReason,
            LocalDateTime fetchedAt) {
        this.paymentKey = paymentKey;
        this.orderIdExternal = orderIdExternal;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.supplyAmount = supplyAmount;
        this.vat = vat;
        this.payOutAmount = payOutAmount;
        this.approvedAt = approvedAt;
        this.soldDate = soldDate;
        this.paidOutDate = paidOutDate;
        this.cancelTransaction = cancelTransaction;
        this.reconciliationStatus = reconciliationStatus;
        this.reconciliationReason = reconciliationReason;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() { return id; }
    public String getTransactionKey() { return transactionKey; }
    public String getPaymentKey() { return paymentKey; }
    public String getOrderIdExternal() { return orderIdExternal; }
    public String getPaymentMethod() { return paymentMethod; }
    public long getAmount() { return amount; }
    public long getFeeAmount() { return feeAmount; }
    public long getSupplyAmount() { return supplyAmount; }
    public long getVat() { return vat; }
    public long getPayOutAmount() { return payOutAmount; }
    public String getApprovedAt() { return approvedAt; }
    public LocalDate getSoldDate() { return soldDate; }
    public LocalDate getPaidOutDate() { return paidOutDate; }
    public boolean isCancelTransaction() { return cancelTransaction; }
    public PaymentSettlementStatus getReconciliationStatus() { return reconciliationStatus; }
    public String getReconciliationReason() { return reconciliationReason; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
