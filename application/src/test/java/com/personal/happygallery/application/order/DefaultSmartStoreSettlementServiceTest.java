package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.SmartStoreSettlementSyncStateService.ClaimedDate;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider.SettlementItem;
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
}
