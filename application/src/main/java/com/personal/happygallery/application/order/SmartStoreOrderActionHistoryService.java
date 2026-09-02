package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ActionHistoryResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.AdminActor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderActionHistoryPort;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreOrderActionHistoryService {

    private final SmartStoreOrderActionHistoryPort historyPort;
    private final Clock clock;

    SmartStoreOrderActionHistoryService(
            SmartStoreOrderActionHistoryPort historyPort,
            Clock clock) {
        this.historyPort = historyPort;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long start(
            String productOrderId,
            SmartStoreOrderAction action,
            String requestSummary,
            AdminActor actor) {
        SmartStoreOrderActionHistory history = historyPort.save(new SmartStoreOrderActionHistory(
                productOrderId,
                action,
                requestSummary,
                actor.adminUserId(),
                actor.name(),
                LocalDateTime.now(clock)));
        return history.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeed(long historyId) {
        SmartStoreOrderActionHistory history = history(historyId);
        history.succeed(LocalDateTime.now(clock));
        historyPort.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reject(long historyId, String resultCode, String resultMessage) {
        SmartStoreOrderActionHistory history = history(historyId);
        history.reject(resultCode, resultMessage, LocalDateTime.now(clock));
        historyPort.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markResultUnknown(long historyId, String resultMessage) {
        SmartStoreOrderActionHistory history = history(historyId);
        history.markResultUnknown(resultMessage, LocalDateTime.now(clock));
        historyPort.save(history);
    }

    @Transactional(readOnly = true)
    public List<ActionHistoryResult> list(String productOrderId) {
        return historyPort.findRecentByProductOrderId(productOrderId).stream()
                .map(history -> new ActionHistoryResult(
                        history.getId(),
                        history.getAction(),
                        history.getStatus(),
                        history.getRequestSummary(),
                        history.getResultCode(),
                        history.getResultMessage(),
                        history.getChangedByAdminId(),
                        history.getChangedBy(),
                        history.getRequestedAt(),
                        history.getCompletedAt()))
                .toList();
    }

    private SmartStoreOrderActionHistory history(long historyId) {
        return historyPort.findByIdWithLock(historyId)
                .orElseThrow(() -> new IllegalStateException("스마트스토어 주문 처리 이력이 없습니다."));
    }
}
