package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.domain.payment.RefundStatus;

/**
 * 8회권 환불 유스케이스.
 *
 * <p>관리자가 전체 환불을 요청하면 잔여 크레딧, 자동 취소한 미래 예약 크레딧,
 * PG 환불 요청을 함께 처리한다.
 */
public interface PassRefundUseCase {

    record PassRefundResult(int canceledBookings,
                            int refundCredits,
                            long refundAmount,
                            Long refundId,
                            RefundStatus refundStatus) {}

    PassRefundResult refundPass(Long passId);
}
