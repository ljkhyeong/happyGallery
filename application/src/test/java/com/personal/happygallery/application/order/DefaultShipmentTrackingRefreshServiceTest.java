package com.personal.happygallery.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.personal.happygallery.application.batch.BatchScheduler;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.KoreaPostTrackingLookup;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

class DefaultShipmentTrackingRefreshServiceTest {
    @Test
    @DisplayName("우체국 연동을 끄면 DB 조회와 배송 갱신을 실행하지 않는다")
    void disabledSkipsWork() {
        FulfillmentPort fulfillments = mock(FulfillmentPort.class);
        var transactions = mock(ShipmentTrackingRefreshTransactionService.class);
        var updates = mock(ShipmentTrackingWebhookUseCase.class);
        var service = new DefaultShipmentTrackingRefreshService(fulfillments,
                mock(KoreaPostTrackingLookup.class), transactions, updates, mock(Clock.class));

        assertThat(service.refreshShipments().successCount()).isZero();
        verifyNoInteractions(fulfillments, transactions, updates);
    }

    @Test
    @DisplayName("우체국 조회 스케줄은 서울 시각 매시 정각과 30분에 실행된다")
    void scheduledAtHalfHourIntervals() throws NoSuchMethodException {
        Scheduled schedule = BatchScheduler.class.getMethod("runShipmentTrackingRefresh")
                .getAnnotation(Scheduled.class);
        assertThat(schedule.zone()).isEqualTo("Asia/Seoul");
        CronExpression cron = CronExpression.parse(schedule.cron());
        LocalDateTime next = cron.next(LocalDateTime.of(2026, 9, 12, 10, 1));
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 9, 12, 10, 30));
        assertThat(cron.next(next)).isEqualTo(LocalDateTime.of(2026, 9, 12, 11, 0));
    }
}
