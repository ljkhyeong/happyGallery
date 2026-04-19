package com.personal.happygallery.application.pass.port.in;

/**
 * 8회권 환불 유스케이스.
 *
 * <p>관리자가 수동으로 전체 환불을 처리한다.
 */
public interface PassRefundUseCase {

    record PassRefundResult(int canceledBookings, int refundCredits, long refundAmount) {}

    PassRefundResult refundPass(Long passId);
}
