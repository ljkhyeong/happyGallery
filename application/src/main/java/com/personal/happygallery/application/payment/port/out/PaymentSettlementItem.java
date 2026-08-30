package com.personal.happygallery.application.payment.port.out;

import java.time.LocalDate;

public record PaymentSettlementItem(
        String transactionKey,
        String paymentKey,
        String orderId,
        String method,
        long amount,
        long feeAmount,
        long supplyAmount,
        long vat,
        long payOutAmount,
        String approvedAt,
        LocalDate soldDate,
        LocalDate paidOutDate,
        boolean cancelTransaction
) {}
