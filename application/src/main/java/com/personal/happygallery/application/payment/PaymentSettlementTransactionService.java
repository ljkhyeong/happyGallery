package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentSettlementItem;
import com.personal.happygallery.application.payment.port.out.PaymentSettlementPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentSettlement;
import com.personal.happygallery.domain.payment.PaymentSettlementStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentSettlementTransactionService {

    private final PaymentSettlementPort settlementPort;
    private final PaymentAttemptReaderPort paymentAttemptReader;
    private final RefundPort refundPort;
    private final Clock clock;

    PaymentSettlementTransactionService(
            PaymentSettlementPort settlementPort,
            PaymentAttemptReaderPort paymentAttemptReader,
            RefundPort refundPort,
            Clock clock) {
        this.settlementPort = settlementPort;
        this.paymentAttemptReader = paymentAttemptReader;
        this.refundPort = refundPort;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentSettlementStatus reconcile(PaymentSettlementItem item) {
        Reconciliation reconciliation = item.cancelTransaction()
                ? reconcileCancel(item)
                : reconcilePayment(item);
        PaymentSettlement settlement = settlementPort.findByTransactionKey(item.transactionKey())
                .orElseGet(() -> PaymentSettlement.create(item.transactionKey()));
        settlement.synchronize(
                item.paymentKey(),
                item.orderId(),
                item.method(),
                item.amount(),
                item.feeAmount(),
                item.supplyAmount(),
                item.vat(),
                item.payOutAmount(),
                item.approvedAt(),
                item.soldDate(),
                item.paidOutDate(),
                item.cancelTransaction(),
                reconciliation.status(),
                reconciliation.reason(),
                LocalDateTime.now(clock));
        settlementPort.save(settlement);
        return reconciliation.status();
    }

    private Reconciliation reconcilePayment(PaymentSettlementItem item) {
        PaymentAttempt attempt = paymentAttemptReader.findByConfirmedPaymentKey(item.paymentKey())
                .orElse(null);
        if (attempt == null) {
            return Reconciliation.of(
                    PaymentSettlementStatus.LOCAL_PAYMENT_NOT_FOUND,
                    "같은 paymentKey의 로컬 결제 승인을 찾지 못했습니다.");
        }
        if (!Objects.equals(attempt.getOrderIdExternal(), item.orderId())) {
            return Reconciliation.of(
                    PaymentSettlementStatus.IDENTIFIER_MISMATCH,
                    "PG orderId가 로컬 결제번호와 일치하지 않습니다.");
        }
        if (attempt.getAmount() != item.amount()) {
            return Reconciliation.of(
                    PaymentSettlementStatus.AMOUNT_MISMATCH,
                    "PG 정산 금액이 로컬 결제 금액과 일치하지 않습니다.");
        }
        return Reconciliation.matched();
    }

    private Reconciliation reconcileCancel(PaymentSettlementItem item) {
        Refund refund = refundPort.findByRefundTransactionKey(item.transactionKey()).orElse(null);
        if (refund == null) {
            return Reconciliation.of(
                    PaymentSettlementStatus.LOCAL_REFUND_NOT_FOUND,
                    "같은 거래키의 로컬 환불 완료 이력을 찾지 못했습니다.");
        }
        if (refund.getAmount() != item.amount()) {
            return Reconciliation.of(
                    PaymentSettlementStatus.AMOUNT_MISMATCH,
                    "PG 취소 정산 금액이 로컬 환불 금액과 일치하지 않습니다.");
        }
        return Reconciliation.matched();
    }

    private record Reconciliation(PaymentSettlementStatus status, String reason) {
        private static Reconciliation matched() {
            return new Reconciliation(PaymentSettlementStatus.MATCHED, null);
        }

        private static Reconciliation of(PaymentSettlementStatus status, String reason) {
            return new Reconciliation(status, reason);
        }
    }
}
