package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 조치 필요 환불 재처리 — 운영자 수동 트리거 */
@Service
public class DefaultRefundRetryService implements RefundRetryUseCase {

    private final RefundPort refundPort;
    private final RefundExecutionService refundExecutionService;

    public DefaultRefundRetryService(RefundPort refundPort,
                                     RefundExecutionService refundExecutionService) {
        this.refundPort = refundPort;
        this.refundExecutionService = refundExecutionService;
    }

    /** 조치 필요 상태인 특정 환불을 재처리한다. */
    @Override
    public Refund retry(Long refundId) {
        return refundExecutionService.retryRefund(refundId);
    }

    /** 실패·재시도 대기·상태 확인 필요 환불 목록 조회 */
    @Override
    @Transactional(readOnly = true)
    public List<Refund> listFailed() {
        return refundPort.findActionRequired();
    }
}
