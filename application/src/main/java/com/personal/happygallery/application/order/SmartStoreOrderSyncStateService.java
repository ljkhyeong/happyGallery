package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderSyncStatePort;
import com.personal.happygallery.domain.order.SmartStoreOrderSyncState;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreOrderSyncStateService {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private final SmartStoreOrderSyncStatePort statePort;
    private final Clock clock;

    SmartStoreOrderSyncStateService(SmartStoreOrderSyncStatePort statePort, Clock clock) {
        this.statePort = statePort;
        this.clock = clock;
    }

    @Transactional
    public void skipDisabledPeriod() {
        SmartStoreOrderSyncState state = lockedState();
        if (state.getProcessingStartedAt() == null) {
            state.complete(LocalDateTime.now(clock), null);
            statePort.save(state);
        }
    }

    @Transactional
    public Optional<ClaimedCursor> claim() {
        LocalDateTime now = LocalDateTime.now(clock);
        SmartStoreOrderSyncState state = statePort
                .findByIdWithLock(SmartStoreOrderSyncState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("스마트스토어 주문 동기화 커서가 없습니다."));
        if (!state.claim(now, now.minus(PROCESSING_TIMEOUT))) {
            return Optional.empty();
        }
        statePort.save(state);
        return Optional.of(new ClaimedCursor(
                new ChangeCursor(state.getLastChangedFrom(), state.getMoreSequence()), now));
    }

    @Transactional
    public void complete(ClaimedCursor claimed, ChangeCursor next) {
        SmartStoreOrderSyncState state = lockedState();
        if (!Objects.equals(state.getProcessingStartedAt(), claimed.processingStartedAt())) {
            return;
        }
        state.complete(next.changedFrom(), next.moreSequence());
        statePort.save(state);
    }

    @Transactional
    public void release(ClaimedCursor claimed) {
        SmartStoreOrderSyncState state = lockedState();
        if (!Objects.equals(state.getProcessingStartedAt(), claimed.processingStartedAt())) {
            return;
        }
        state.release();
        statePort.save(state);
    }

    private SmartStoreOrderSyncState lockedState() {
        return statePort.findByIdWithLock(SmartStoreOrderSyncState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("스마트스토어 주문 동기화 커서가 없습니다."));
    }

    record ClaimedCursor(ChangeCursor cursor, LocalDateTime processingStartedAt) {}
}
