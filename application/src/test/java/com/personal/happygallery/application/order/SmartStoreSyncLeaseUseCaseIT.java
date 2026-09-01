package com.personal.happygallery.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderSyncStatePort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementSyncStatePort;
import com.personal.happygallery.domain.time.Clocks;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @CsvSource({",true", "299999999,false", "300000000,true", "300000001,true"})
    @DisplayName("연동 중지 기간은 선점이 없거나 5분이 지난 경우에만 건너뛰고 이전 실행의 완료를 거절한다")
    void skipDisabledPeriod_protectsActiveLeaseAndExpiresAtFiveMinutes(Long elapsedMicros, boolean skipped) {
        LocalDateTime now = NOW.truncatedTo(ChronoUnit.MICROS);
        LocalDateTime previousFrom = now.minusHours(2);
        LocalDateTime startedAt = elapsedMicros == null ? null : now.minus(elapsedMicros, ChronoUnit.MICROS);
        jdbc.update("""
                UPDATE smartstore_order_sync_state
                SET last_changed_from = ?, more_sequence = 'previous-page', processing_started_at = ? WHERE id = 1
                """, previousFrom, startedAt);
        var service = new SmartStoreOrderSyncStateService(orderStatePort, clock(NOW));
        var tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> service.skipDisabledPeriod());

        var state = tx.execute(status -> orderStatePort.findByIdWithLock(1L).orElseThrow());
        assertSoftly(softly -> {
            softly.assertThat(state.getLastChangedFrom()).isEqualTo(skipped ? now : previousFrom);
            softly.assertThat(state.getMoreSequence()).isEqualTo(skipped ? null : "previous-page");
            softly.assertThat(state.getProcessingStartedAt()).isEqualTo(skipped ? null : startedAt);
            softly.assertThat(state.getIntegrationEnabled()).isFalse();
        });
        if (skipped && startedAt != null) {
            var previous = new SmartStoreOrderSyncStateService.ClaimedCursor(
                    new ChangeCursor(previousFrom, "previous-page"), startedAt);
            Boolean completed = tx.execute(status -> service.complete(previous,
                    new ChangeCursor(previousFrom.plusMinutes(1), "late-page")));
            assertThat(completed).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("재기동은 이전 연동 상태가 중지였을 때만 활성화 시각부터 주문을 수집한다")
    void claim_startsAtActivationOnlyAfterDisabled(boolean previouslyEnabled) {
        LocalDateTime now = NOW.truncatedTo(ChronoUnit.MICROS);
        LocalDateTime previousFrom = now.minusHours(2);
        jdbc.update("""
                UPDATE smartstore_order_sync_state
                SET last_changed_from = ?, more_sequence = 'previous-page', processing_started_at = NULL,
                    integration_enabled = ?
                WHERE id = 1
                """, previousFrom, previouslyEnabled);
        var service = new SmartStoreOrderSyncStateService(orderStatePort, clock(NOW));
        var tx = new TransactionTemplate(transactionManager);

        var claimed = tx.execute(status -> service.claim().orElseThrow());

        assertSoftly(softly -> {
            softly.assertThat(claimed.cursor().changedFrom()).isEqualTo(previouslyEnabled ? previousFrom : now);
            softly.assertThat(claimed.cursor().moreSequence()).isEqualTo(previouslyEnabled ? "previous-page" : null);
            softly.assertThat(claimed.processingStartedAt()).isEqualTo(now);
        });
        var state = tx.execute(status -> orderStatePort.findByIdWithLock(1L).orElseThrow());
        assertThat(state.getIntegrationEnabled()).isTrue();
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
