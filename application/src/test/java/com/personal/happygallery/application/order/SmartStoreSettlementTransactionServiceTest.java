package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider.SettlementItem;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import com.personal.happygallery.domain.order.SmartStoreSettlementStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartStoreSettlementTransactionServiceTest {

    @Test
    @DisplayName("주문 상세와 정산 예정 금액이 다르면 관리자 확인 대상으로 저장한다")
    void reconcile_differentAmount_savesMismatch() {
        SmartStoreSettlementPort settlementPort = mock(SmartStoreSettlementPort.class);
        SmartStoreProductOrderPort orderPort = mock(SmartStoreProductOrderPort.class);
        SmartStoreSettlementTransactionService service = new SmartStoreSettlementTransactionService(
                settlementPort, orderPort,
                Clock.fixed(Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC));
        SmartStoreProductOrder order = order(67000L);
        when(orderPort.findByProductOrderId("po-1")).thenReturn(Optional.of(order));
        when(settlementPort.findByEntryKey(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        SmartStoreSettlementStatus status = service.reconcile(item(66000L));

        ArgumentCaptor<SmartStoreSettlementEntry> captor =
                ArgumentCaptor.forClass(SmartStoreSettlementEntry.class);
        verify(settlementPort).save(captor.capture());
        assertThat(status).isEqualTo(SmartStoreSettlementStatus.AMOUNT_MISMATCH);
        assertThat(captor.getValue().getReconciliationStatus())
                .isEqualTo(SmartStoreSettlementStatus.AMOUNT_MISMATCH);
        assertThat(captor.getValue().getReconciliationReason()).contains("정산 예정 금액");
    }

    private static SmartStoreProductOrder order(long expectedSettlementAmount) {
        LocalDateTime changedAt = LocalDateTime.of(2026, 8, 29, 12, 0);
        return new SmartStoreProductOrder(
                "po-1", "order-1", 123L, null, "가죽 지갑", null, null,
                "PURCHASE_DECIDED", "OK", null, null, 1, 1, "PURCHASE_DECIDED",
                changedAt.minusDays(2), changedAt, null, "DELIVERY", "CJ대한통운", "1234",
                70000L, 70000L, 1000L, 2000L, 0L, expectedSettlementAmount);
    }

    private static SettlementItem item(long expectedSettlementAmount) {
        LocalDate payDate = LocalDate.of(2026, 8, 29);
        return new SettlementItem(
                "po-1", "order-1", "PROD_ORDER", "NORMAL_SETTLE_ORIGINAL", "가죽 지갑",
                70000L, 1000L, 2000L, 0L, expectedSettlementAmount,
                payDate.minusDays(2), payDate, payDate, payDate);
    }
}
