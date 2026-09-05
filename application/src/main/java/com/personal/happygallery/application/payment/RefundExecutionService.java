package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 요청 이력 저장 및 실행 요청 서비스.
 *
 * <p>환불 요청 레코드는 호출 유스케이스 트랜잭션에 참여하고, PG 환불 호출은 커밋 이후
 * 발행되는 실행 이벤트가 담당한다. 운영자 재시도는 트랜잭션 밖에서 동기 실행한다.
 * 예약·주문·8회권의 환불을 공통 처리한다.
 */
@Service
public class RefundExecutionService {

    private final RefundPort refundPort;
    private final RefundTransactionService refundTransactionService;
    private final RefundDispatcher refundDispatcher;
    private final ApplicationEventPublisher eventPublisher;

    public RefundExecutionService(RefundPort refundPort,
                                  RefundTransactionService refundTransactionService,
                                  RefundDispatcher refundDispatcher,
                                  ApplicationEventPublisher eventPublisher) {
        this.refundPort = refundPort;
        this.refundTransactionService = refundTransactionService;
        this.refundDispatcher = refundDispatcher;
        this.eventPublisher = eventPublisher;
    }

    /** 환불 요청 이력을 저장하고 커밋 이후 PG 환불 실행을 예약한다. 반환값은 PG 결과 반영 전 요청 이력이다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestOrderRefund(Long orderId, long amount, String paymentKey) {
        return requestOrderRefund(
                orderId, amount, amount, 0L, 0L, false, paymentKey);
    }

    /** 주문의 PG 취소액과 고객 혜택 반환 스냅샷을 저장하고 실행을 예약한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestOrderRefund(
            Long orderId,
            long pgRefundAmount,
            long customerRefundAmount,
            long rewardRestoreAmount,
            long rewardRevokeAmount,
            boolean restoreCoupon,
            String paymentKey) {
        Refund refund = refundPort.save(Refund.forOrder(
                orderId,
                pgRefundAmount,
                customerRefundAmount,
                rewardRestoreAmount,
                rewardRevokeAmount,
                restoreCoupon,
                paymentKey));
        publishExecutionRequested(refund.getId(), "orderId=" + orderId);
        return refund;
    }

    /** 주문 클레임별 환불 요청을 저장하고 커밋 이후 PG 환불 실행을 예약한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestOrderClaimRefund(
            Long orderId, Long orderClaimId, long amount, String paymentKey) {
        return requestOrderClaimRefund(
                orderId, orderClaimId, amount, amount, 0L, 0L, paymentKey);
    }

    /** 클레임 승인 시 계산된 PG·적립금 반환과 적립 회수액을 저장하고 실행을 예약한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestOrderClaimRefund(
            Long orderId,
            Long orderClaimId,
            long pgRefundAmount,
            long customerRefundAmount,
            long rewardRestoreAmount,
            long rewardRevokeAmount,
            String paymentKey) {
        Refund refund = refundPort.save(
                Refund.forOrderClaim(
                        orderId,
                        orderClaimId,
                        pgRefundAmount,
                        customerRefundAmount,
                        rewardRestoreAmount,
                        rewardRevokeAmount,
                        paymentKey));
        publishExecutionRequested(refund.getId(), "orderClaimId=" + orderClaimId);
        return refund;
    }

    /** 환불 요청 이력을 저장하고 커밋 이후 PG 환불 실행을 예약한다. 반환값은 PG 결과 반영 전 요청 이력이다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestBookingRefund(Booking booking, long amount) {
        Refund refund = refundPort.save(Refund.forBooking(booking, amount));
        publishExecutionRequested(refund.getId(), "bookingId=" + booking.getId());
        return refund;
    }

    /** 환불 요청 이력을 저장하고 커밋 이후 PG 환불 실행을 예약한다. 반환값은 PG 결과 반영 전 요청 이력이다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestPassRefund(Long passPurchaseId, long amount, String paymentKey) {
        Refund refund = refundPort.save(Refund.forPass(passPurchaseId, amount, paymentKey));
        publishExecutionRequested(refund.getId(), "passPurchaseId=" + passPurchaseId);
        return refund;
    }

    /** PG 승인 후 도메인 생성 실패 결제를 기존 환불 재시도 경로로 보상한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Refund requestPaymentAttemptRefund(Long paymentAttemptId, long amount, String paymentKey) {
        Refund refund = refundPort.save(Refund.forPaymentAttempt(paymentAttemptId, amount, paymentKey));
        publishExecutionRequested(refund.getId(), "paymentAttemptId=" + paymentAttemptId);
        return refund;
    }

    public Refund retryRefund(Long refundId) {
        refundTransactionService.requestManualRetry(refundId);
        return refundDispatcher.dispatch(refundId, "retry refundId=" + refundId);
    }

    private void publishExecutionRequested(Long refundId, String target) {
        eventPublisher.publishEvent(new RefundExecutionRequestedEvent(refundId, target));
    }
}
