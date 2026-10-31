package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 승인된 결제의 도메인 생성(fulfillment)과 실패 보상을 한 순서로 실행한다.
 *
 * <p>confirm과 관리자 대사가 공유하는 비트랜잭션 협력 객체다. 각 단계는
 * {@link PaymentConfirmFulfillmentTransactionService}의 {@code REQUIRES_NEW} 트랜잭션으로 실행하고,
 * 보상 요청 저장까지 실패하면 원래 fulfillment 예외에 suppressed 원인으로 보존한 뒤 ERROR 로그를 남긴다.
 */
@Component
class PaymentFulfillmentCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PaymentFulfillmentCoordinator.class);

    private final PaymentConfirmFulfillmentTransactionService fulfillmentTransactionService;

    PaymentFulfillmentCoordinator(PaymentConfirmFulfillmentTransactionService fulfillmentTransactionService) {
        this.fulfillmentTransactionService = fulfillmentTransactionService;
    }

    /**
     * 도메인 생성을 실행하고 실패하면 원래 예외를 다시 던진다. 유료 결제는 확정 paymentKey로 보상 환불을 요청하고,
     * amount=0 결제는 외부 결제가 없으므로 실패로만 기록한다.
     */
    ConfirmResult fulfillOrCompensate(Long attemptId,
                                      String orderId,
                                      long amount,
                                      String confirmedPaymentKey,
                                      String compensationReason) {
        try {
            return fulfillmentTransactionService.fulfillAndConfirm(attemptId);
        } catch (RuntimeException fulfillmentFailure) {
            if (amount > 0L) {
                compensateAfterFulfillmentFailure(
                        attemptId, orderId, confirmedPaymentKey, compensationReason, fulfillmentFailure);
            } else {
                recordZeroAmountFailure(attemptId, orderId, fulfillmentFailure);
            }
            throw fulfillmentFailure;
        }
    }

    private void compensateAfterFulfillmentFailure(Long attemptId,
                                                   String orderId,
                                                   String confirmedPaymentKey,
                                                   String reason,
                                                   RuntimeException originalFailure) {
        try {
            boolean requested = fulfillmentTransactionService.requestCompensationAfterFulfillmentFailure(
                    attemptId, confirmedPaymentKey, reason);
            if (!requested) {
                log.warn("완료되었거나 다른 요청이 처리한 결제의 보상 환불을 건너뜁니다 [attemptId={}, orderId={}]",
                        attemptId, orderId);
            }
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
            log.error("PG 승인 결제의 보상 환불 요청 저장 실패 — 자동 복구 대상 유지 "
                            + "[attemptId={}, orderId={}, type={}]",
                    attemptId, orderId, compensationFailure.getClass().getSimpleName());
        }
    }

    private void recordZeroAmountFailure(Long attemptId,
                                         String orderId,
                                         RuntimeException originalFailure) {
        try {
            boolean recorded = fulfillmentTransactionService.tryMarkZeroAmountFulfillmentFailed(
                    attemptId, "도메인 생성에 실패했습니다.");
            if (!recorded) {
                log.warn("stale amount=0 결제 실패 결과를 건너뜁니다 [attemptId={}, orderId={}]",
                        attemptId, orderId);
            }
        } catch (RuntimeException stateFailure) {
            originalFailure.addSuppressed(stateFailure);
            log.error("amount=0 결제 실패 상태 저장 실패 [attemptId={}, orderId={}, type={}]",
                    attemptId, orderId, stateFailure.getClass().getSimpleName());
        }
    }
}
