package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreSettlementSyncStatePort;
import com.personal.happygallery.domain.order.SmartStoreSettlementSyncState;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreSettlementSyncStateService {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);

    private final SmartStoreSettlementSyncStatePort statePort;
    private final Clock clock;

    SmartStoreSettlementSyncStateService(
            SmartStoreSettlementSyncStatePort statePort, Clock clock) {
        this.statePort = statePort;
        this.clock = clock;
    }

    @Transactional
    Optional<ClaimedDate> claim() {
        LocalDateTime now = LocalDateTime.now(clock);
        SmartStoreSettlementSyncState state = lockedState();
        if (!state.claim(now, now.minus(PROCESSING_TIMEOUT))) {
            return Optional.empty();
        }
        LocalDate payDate = state.dateToProcess(LocalDate.now(clock));
        return Optional.of(new ClaimedDate(payDate, now));
    }

    @Transactional
    void complete(ClaimedDate claimed) {
        SmartStoreSettlementSyncState state = lockedState();
        if (!Objects.equals(state.getProcessingStartedAt(), claimed.processingStartedAt())) {
            return;
        }
        state.complete(claimed.payDate());
    }

    @Transactional
    void release(ClaimedDate claimed) {
        SmartStoreSettlementSyncState state = lockedState();
        if (!Objects.equals(state.getProcessingStartedAt(), claimed.processingStartedAt())) {
            return;
        }
        state.release();
    }

    private SmartStoreSettlementSyncState lockedState() {
        return statePort.findByIdWithLock(SmartStoreSettlementSyncState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("스마트스토어 정산 동기화 커서가 없습니다."));
    }

    record ClaimedDate(LocalDate payDate, LocalDateTime processingStartedAt) {}
}
