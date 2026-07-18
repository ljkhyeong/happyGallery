package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.booking.Refund;
import java.util.List;

/**
 * 환불 재시도 유스케이스.
 *
 * <p>운영자가 실패·재시도 대기·상태 확인 필요 환불을 조회하고 수동으로 재시도한다.
 */
public interface RefundRetryUseCase {

    Refund retry(Long refundId);

    List<Refund> listFailed();
}
