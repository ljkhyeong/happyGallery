package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.SmartStoreSettlementSyncStateService.ClaimedDate;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider.SettlementItem;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.SmartStoreSettlementStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSmartStoreSettlementServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("저장된 지급일 커서를 처리하고 성공한 날짜를 다음 날짜로 넘긴다")
    void synchronizeRecent_completesClaimedDate() {
        SmartStoreSettlementProvider provider = mock(SmartStoreSettlementProvider.class);
        SmartStoreSettlementTransactionService transactionService =
                mock(SmartStoreSettlementTransactionService.class);
        SmartStoreSettlementSyncStateService stateService =
                mock(SmartStoreSettlementSyncStateService.class);
        SmartStoreSettlementPort settlementPort = mock(SmartStoreSettlementPort.class);
        ClaimedDate claimed = new ClaimedDate(
                LocalDate.of(2026, 8, 29), LocalDateTime.of(2026, 8, 29, 12, 0));
        SettlementItem item = mock(SettlementItem.class);
        when(provider.isEnabled()).thenReturn(true);
        when(stateService.claim()).thenReturn(Optional.of(claimed));
        when(provider.findByPayDate(claimed.payDate())).thenReturn(List.of(item));
        when(transactionService.reconcile(item)).thenReturn(SmartStoreSettlementStatus.MATCHED);

        var service = new DefaultSmartStoreSettlementService(
                provider, transactionService, settlementPort, stateService, CLOCK);

        var result = service.synchronizeRecent();

        assertThat(result.successCount()).isEqualTo(1);
        verify(stateService).complete(claimed);
    }

    @Test
    @DisplayName("수동 정산 재동기화는 31일을 넘는 기간을 거절한다")
    void synchronize_rejectsRangeLongerThanThirtyOneDays() {
        SmartStoreSettlementProvider provider = mock(SmartStoreSettlementProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        var service = new DefaultSmartStoreSettlementService(
                provider,
                mock(SmartStoreSettlementTransactionService.class),
                mock(SmartStoreSettlementPort.class),
                mock(SmartStoreSettlementSyncStateService.class),
                CLOCK);

        assertThatThrownBy(() -> service.synchronize(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("31일");
    }

    @Test
    @DisplayName("회계 자료의 부가세 조회 종료일은 전월 말로 제한한다")
    void accounting_clipsVatRangeToPreviousMonth() {
        SmartStoreSettlementProvider provider = mock(SmartStoreSettlementProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        LocalDate from = LocalDate.of(2026, 7, 20);
        LocalDate to = LocalDate.of(2026, 8, 10);
        when(provider.findDailySettlements(from, to)).thenReturn(List.of());
        when(provider.findCommissionDetails(from, to)).thenReturn(List.of());
        when(provider.findDailyVat(from, LocalDate.of(2026, 7, 31))).thenReturn(List.of());
        var service = new DefaultSmartStoreSettlementService(
                provider,
                mock(SmartStoreSettlementTransactionService.class),
                mock(SmartStoreSettlementPort.class),
                mock(SmartStoreSettlementSyncStateService.class),
                CLOCK);

        var report = service.accounting(from, to);

        assertThat(report.vatAvailableThrough()).isEqualTo(LocalDate.of(2026, 7, 31));
        verify(provider).findDailyVat(from, LocalDate.of(2026, 7, 31));
    }

    @Test
    @DisplayName("연동이 꺼져 있으면 관리자 화면이 구분할 수 있는 전용 오류 코드로 거절한다")
    void accounting_disabledIntegration_rejectsWithDedicatedCode() {
        SmartStoreSettlementProvider provider = mock(SmartStoreSettlementProvider.class);
        when(provider.isEnabled()).thenReturn(false);
        var service = new DefaultSmartStoreSettlementService(
                provider,
                mock(SmartStoreSettlementTransactionService.class),
                mock(SmartStoreSettlementPort.class),
                mock(SmartStoreSettlementSyncStateService.class),
                CLOCK);

        assertThatThrownBy(() -> service.accounting(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SMARTSTORE_DISABLED));
    }
}
