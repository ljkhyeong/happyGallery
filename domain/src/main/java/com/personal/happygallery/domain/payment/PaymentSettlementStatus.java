package com.personal.happygallery.domain.payment;

public enum PaymentSettlementStatus {
    MATCHED,
    LOCAL_PAYMENT_NOT_FOUND,
    LOCAL_REFUND_NOT_FOUND,
    IDENTIFIER_MISMATCH,
    AMOUNT_MISMATCH
}
