package com.personal.happygallery.app.payment;

import com.personal.happygallery.app.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.app.payment.port.out.RefundPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 환불 실패 재시도 — 운영자 수동 트리거 */
@Service
@Transactional
public class DefaultRefundRetryService implements RefundRetryUseCase {

    private final RefundPort refundPort;
    private final RefundExecutionService refundExecutionService;

    public DefaultRefundRetryService(RefundPort refundPort,
                                     RefundExecutionService refundExecutionService) {
        this.refundPort = refundPort;
        this.refundExecutionService = refundExecutionService;
    }

    /** FAILED 상태인 특정 환불을 재시도한다. */
    public void retry(Long refundId) {
        Refund refund = refundPort.findById(refundId)
                .orElseThrow(NotFoundException.supplier("환불"));

        if (refund.getStatus() != RefundStatus.FAILED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "FAILED 상태 환불만 재시도 가능합니다. (현재: " + refund.getStatus() + ")");
        }

        refundExecutionService.executeRefund(refund, "retry refundId=" + refundId);
    }

    /** FAILED 상태인 환불 목록 조회 */
    @Transactional(readOnly = true)
    public List<Refund> listFailed() {
        return refundPort.findByStatus(RefundStatus.FAILED);
    }
}
