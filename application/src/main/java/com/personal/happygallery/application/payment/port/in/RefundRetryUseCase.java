package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.booking.Refund;
import java.util.List;

/**
 * 환불 재시도 유스케이스.
 *
 * <p>운영자가 FAILED 상태 환불을 수동으로 재시도한다.
 */
public interface RefundRetryUseCase {

    void retry(Long refundId);

    List<Refund> listFailed();
}
