package com.personal.happygallery.application.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderSyncStatePort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementSyncStatePort;
import com.personal.happygallery.domain.time.Clocks;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@UseCaseIT
class SmartStoreSyncLeaseUseCaseIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 31, 12, 0, 0, 123456789);

    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired SmartStoreOrderSyncStatePort orderStatePort;
    @Autowired SmartStoreSettlementSyncStatePort settlementStatePort;

    @Test
    @DisplayName("주문 수집은 나노초 시각도 DB 왕복 후 완료·해제하고 만료된 실행은 새 선점을 변경하지 못한다")
    void orderLease_survivesDatabasePrecisionAndRejectsPreviousOwner() {
        jdbc.update("""
                UPDATE smartstore_order_sync_state
                SET last_changed_from = ?, more_sequence = NULL, processing_started_at = NULL WHERE id = 1
                """, NOW.minusMinutes(1));
        var service = new SmartStoreOrderSyncStateService(orderStatePort, clock(NOW));
        var tx = new TransactionTemplate(transactionManager);
        var first = tx.execute(status -> service.claim().orElseThrow());
        Boolean completed = tx.execute(status -> service.complete(first, new ChangeCursor(NOW, null)));
        assertThat(completed).isTrue();

        var previous = tx.execute(status -> service.claim().orElseThrow());
        var nextService = new SmartStoreOrderSyncStateService(orderStatePort, clock(NOW.plusMinutes(6)));
        var next = tx.execute(status -> nextService.claim().orElseThrow());
        Boolean previousCompleted = tx.execute(status -> service.complete(previous, new ChangeCursor(NOW, null)));
        assertThat(previousCompleted).isFalse();
        tx.executeWithoutResult(status -> service.release(previous));
        assertThat(orderProcessingStartedAt()).isEqualTo(next.processingStartedAt());
        tx.executeWithoutResult(status -> nextService.release(next));
        assertThat(orderProcessingStartedAt()).isNull();
    }

    @Test
    @DisplayName("정산 수집은 나노초 시각도 처리 날짜를 전진시키고 이전 실행의 완료·해제를 거절한다")
    void settlementLease_survivesDatabasePrecisionAndRejectsPreviousOwner() {
        LocalDate payDate = NOW.toLocalDate().minusDays(2);
        jdbc.update("""
                UPDATE smartstore_settlement_sync_state
                SET next_pay_date = ?, processing_started_at = NULL WHERE id = 1
                """, payDate);
        var service = new SmartStoreSettlementSyncStateService(settlementStatePort, clock(NOW));
        var tx = new TransactionTemplate(transactionManager);
        var first = tx.execute(status -> service.claim().orElseThrow());
        tx.executeWithoutResult(status -> service.complete(first));
        assertThat(nextPayDate()).isEqualTo(payDate.plusDays(1));
        assertThat(settlementProcessingStartedAt()).isNull();

        var previous = tx.execute(status -> service.claim().orElseThrow());
        var nextService = new SmartStoreSettlementSyncStateService(settlementStatePort, clock(NOW.plusMinutes(11)));
        var next = tx.execute(status -> nextService.claim().orElseThrow());
        tx.executeWithoutResult(status -> service.complete(previous));
        tx.executeWithoutResult(status -> service.release(previous));
        assertThat(nextPayDate()).isEqualTo(payDate.plusDays(1));
        assertThat(settlementProcessingStartedAt()).isEqualTo(next.processingStartedAt());
        tx.executeWithoutResult(status -> nextService.release(next));
        assertThat(settlementProcessingStartedAt()).isNull();
    }

    private LocalDateTime orderProcessingStartedAt() {
        return jdbc.queryForObject("SELECT processing_started_at FROM smartstore_order_sync_state WHERE id = 1", LocalDateTime.class);
    }

    private LocalDateTime settlementProcessingStartedAt() {
        return jdbc.queryForObject("SELECT processing_started_at FROM smartstore_settlement_sync_state WHERE id = 1", LocalDateTime.class);
    }

    private LocalDate nextPayDate() {
        return jdbc.queryForObject("SELECT next_pay_date FROM smartstore_settlement_sync_state WHERE id = 1", LocalDate.class);
    }

    private static Clock clock(LocalDateTime time) {
        return Clock.fixed(time.atZone(Clocks.SEOUL).toInstant(), Clocks.SEOUL);
    }
}
