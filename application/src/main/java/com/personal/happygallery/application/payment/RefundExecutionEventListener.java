package com.personal.happygallery.application.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class RefundExecutionEventListener {

    private static final Logger log = LoggerFactory.getLogger(RefundExecutionEventListener.class);

    private final RefundDispatcher refundDispatcher;

    RefundExecutionEventListener(RefundDispatcher refundDispatcher) {
        this.refundDispatcher = refundDispatcher;
    }

    @Async("refundExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void executeAfterCommit(RefundExecutionRequestedEvent event) {
        try {
            refundDispatcher.dispatch(event.refundId(), event.target());
        } catch (Exception e) {
            log.error("환불 비동기 실행 실패 [refundId={} type={}]",
                    event.refundId(), e.getClass().getSimpleName(), e);
        }
    }
}

record RefundExecutionRequestedEvent(Long refundId, String target) {}
