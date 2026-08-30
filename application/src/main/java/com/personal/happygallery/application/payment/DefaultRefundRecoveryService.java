package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.in.RefundRecoveryUseCase;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultRefundRecoveryService implements RefundRecoveryUseCase {

    private static final int BATCH_SIZE = 10;

    private final RefundPort refundPort;
    private final RefundDispatcher refundDispatcher;
    private final Clock clock;

    public DefaultRefundRecoveryService(RefundPort refundPort,
                                        RefundDispatcher refundDispatcher,
                                        Clock clock) {
        this.refundPort = refundPort;
        this.refundDispatcher = refundDispatcher;
        this.clock = clock;
    }

    @Override
    public BatchResult recoverPendingRefunds() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> refundIds = refundPort.findRecoverableIds(
                now,
                now.minus(RefundTransactionService.PROCESSING_TIMEOUT),
                BATCH_SIZE);
        return BatchExecutor.execute(
                refundIds,
                refundId -> refundId,
                refundId -> {
                    RefundStatus status = refundDispatcher.dispatchRecovery(
                            refundId, "recovery refundId=" + refundId).getStatus();
                    if (status == RefundStatus.PROCESSING) {
                        return false;
                    }
                    if (status != RefundStatus.SUCCEEDED) {
                        throw new RefundRecoveryIncompleteException(status);
                    }
                    return true;
                },
                "환불 복구");
    }

    private static final class RefundRecoveryIncompleteException extends RuntimeException {

        private RefundRecoveryIncompleteException(RefundStatus status) {
            super("환불 복구 미완료 상태: " + status);
        }
    }
}
