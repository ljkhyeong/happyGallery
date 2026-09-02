package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderSyncStatePort;
import com.personal.happygallery.domain.order.SmartStoreOrderSyncState;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreOrderSyncStateService {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private final SmartStoreOrderSyncStatePort statePort;
    private final Clock clock;
    private final LocalDateTime applicationStartedAt;

    SmartStoreOrderSyncStateService(SmartStoreOrderSyncStatePort statePort, Clock clock) {
        this.statePort = statePort;
        this.clock = clock;
        this.applicationStartedAt = now();
    }

    @Transactional
    public void recordEnabledStart() {
        lockedState().recordEnabledStart(applicationStartedAt);
    }

    @Transactional
    public void skipDisabledPeriod() {
        LocalDateTime now = now();
        SmartStoreOrderSyncState state = lockedState();
        state.disable();
        if (state.claim(now, now.minus(PROCESSING_TIMEOUT))) {
            state.complete(now, null);
        }
    }

    @Transactional
    public Optional<ClaimedCursor> claim() {
        LocalDateTime now = now();
        SmartStoreOrderSyncState state = lockedState();
        state.recordEnabledStart(applicationStartedAt);
        if (!state.claimEnabled(now, now.minus(PROCESSING_TIMEOUT))) {
            return Optional.empty();
        }
        return Optional.of(new ClaimedCursor(
                new ChangeCursor(state.getLastChangedFrom(), state.getMoreSequence()), now));
    }

    @Transactional
    public boolean complete(ClaimedCursor claimed, ChangeCursor next) {
        SmartStoreOrderSyncState state = lockedState();
        if (!Objects.equals(state.getProcessingStartedAt(), claimed.processingStartedAt())) {
            return false;
        }
        state.complete(next.changedFrom(), next.moreSequence());
        return true;
    }

    @Transactional
    public void release(ClaimedCursor claimed) {
        SmartStoreOrderSyncState state = lockedState();
        if (!Objects.equals(state.getProcessingStartedAt(), claimed.processingStartedAt())) {
            return;
        }
        state.release();
    }

    private SmartStoreOrderSyncState lockedState() {
        return statePort.findByIdWithLock(SmartStoreOrderSyncState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("스마트스토어 주문 동기화 커서가 없습니다."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    record ClaimedCursor(ChangeCursor cursor, LocalDateTime processingStartedAt) {}
}
