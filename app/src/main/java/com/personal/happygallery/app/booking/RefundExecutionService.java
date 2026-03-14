package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.payment.PaymentProvider;
import com.personal.happygallery.infra.payment.RefundContext;
import com.personal.happygallery.infra.payment.RefundResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 실행/이력 저장 서비스.
 *
 * <p>REQUIRES_NEW 트랜잭션으로 실행해 부모 트랜잭션 롤백과 무관하게 환불 이력을 남긴다.
 */
@Service
public class RefundExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RefundExecutionService.class);

    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final PaymentProvider paymentProvider;

    public RefundExecutionService(RefundRepository refundRepository,
                                  BookingRepository bookingRepository,
                                  PaymentProvider paymentProvider) {
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund processOrderRefund(Long orderId, long amount) {
        Refund refund = refundRepository.save(new Refund(orderId, amount));
        return executeRefund(refund, "orderId=" + orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund processBookingRefund(Long bookingId, long amount) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("예약"));
        Refund refund = refundRepository.save(new Refund(booking, amount));
        return executeRefund(refund, "bookingId=" + bookingId);
    }

    Refund executeRefund(Refund refund, String target) {
        try {
            RefundContext.setOrderId(refund.getOrderId());
            RefundResult result = paymentProvider.refund(refund.getPgRef(), refund.getAmount());
            if (result.success()) {
                refund.markSucceeded(result.pgRef());
            } else {
                log.warn("환불 실패 [{} refundId={}] reason={}", target, refund.getId(), result.failReason());
                refund.markFailed(result.failReason());
            }
        } catch (Exception e) {
            log.error("환불 호출 예외 [{} refundId={}]", target, refund.getId(), e);
            refund.markFailed(e.getMessage());
        } finally {
            RefundContext.clear();
        }
        return refundRepository.save(refund);
    }
}
