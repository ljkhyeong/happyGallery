package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.in.RefundRecoveryUseCase;
import com.personal.happygallery.application.payment.port.out.RefundPort;
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
                    refundDispatcher.dispatch(refundId, "recovery refundId=" + refundId);
                    return true;
                },
                "환불 복구");
    }
}
