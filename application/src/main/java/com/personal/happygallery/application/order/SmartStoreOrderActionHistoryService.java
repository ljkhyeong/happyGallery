package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ActionHistoryResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.AdminActor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderActionHistoryPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import com.personal.happygallery.domain.order.SmartStoreOrderReconciliationOutcome;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNotSent(long historyId, String resultCode, String resultMessage) {
        SmartStoreOrderActionHistory history = history(historyId);
        history.markNotSent(resultCode, resultMessage, LocalDateTime.now(clock));
        historyPort.save(history);
    }

    @Transactional(readOnly = true)
    public List<ActionHistoryResult> list(String productOrderId) {
        return historyPort.findRecentByProductOrderId(productOrderId).stream()
                .map(SmartStoreOrderActionHistoryService::result)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursorPage<ActionHistoryResult> listUnresolved(String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        CursorUtils.CursorParam cursorParam = cursor == null ? null : CursorUtils.decode(cursor);
        List<ActionHistoryResult> results = historyPort.findUnresolvedPage(
                        staleRequestedBefore(),
                        cursorParam == null ? null : cursorParam.timestamp(),
                        cursorParam == null ? null : cursorParam.id(),
                        pageSize + 1)
                .stream()
                .map(SmartStoreOrderActionHistoryService::result)
                .toList();
        return CursorPage.of(results, pageSize, item ->
                CursorUtils.encode(item.requestedAt(), item.id()));
    }

    @Transactional
    public ActionHistoryResult reconcile(
            long historyId,
            SmartStoreOrderReconciliationOutcome outcome,
            String note,
            AdminActor actor) {
        SmartStoreOrderActionHistory history = history(historyId);
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            history.reconcile(
                    outcome, note, actor.adminUserId(), actor.name(), now,
                    now.minus(SmartStoreOrderActionHistory.STALE_REQUEST_AFTER));
        } catch (IllegalStateException exception) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "이미 처리됐거나 아직 진행 중인 스마트스토어 주문 요청입니다.");
        }
        return result(historyPort.save(history));
    }

    private SmartStoreOrderActionHistory history(long historyId) {
        return historyPort.findByIdWithLock(historyId)
                .orElseThrow(() -> new IllegalStateException("스마트스토어 주문 처리 이력이 없습니다."));
    }

    private LocalDateTime staleRequestedBefore() {
        return LocalDateTime.now(clock).minus(SmartStoreOrderActionHistory.STALE_REQUEST_AFTER);
    }

    private static ActionHistoryResult result(SmartStoreOrderActionHistory history) {
        return new ActionHistoryResult(
                history.getId(),
                history.getProductOrderId(),
                history.getAction(),
                history.getStatus(),
                history.getRequestSummary(),
                history.getResultCode(),
                history.getResultMessage(),
                history.getChangedByAdminId(),
                history.getChangedBy(),
                history.getRequestedAt(),
                history.getCompletedAt(),
                history.getReconciliationOutcome(),
                history.getReconciliationNote(),
                history.getReconciledByAdminId(),
                history.getReconciledBy(),
                history.getReconciledAt());
    }
}
