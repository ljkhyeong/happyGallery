package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.PickupReminderTarget;
import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.time.Clocks;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickupDeadlineReminderBatchServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-27T00:00:00Z"),
            Clocks.SEOUL);

    @DisplayName("같은 회원의 픽업 알림도 주문별 멱등키로 각각 요청한다")
    @Test
    void sendPickupDeadlineReminders_sameUserDifferentOrders_requestsEachOrder() {
        FulfillmentPort fulfillmentPort = mock(FulfillmentPort.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        LocalDateTime now = LocalDateTime.now(CLOCK);
        when(fulfillmentPort.findPickupReminderTargets(now, now.plusHours(2)))
                .thenReturn(List.of(
                        new PickupReminderTarget(101L, 10L, null),
                        new PickupReminderTarget(102L, 10L, null)));
        var service = new DefaultPickupDeadlineReminderBatchService(
                fulfillmentPort, outboxService, CLOCK);
        when(outboxService.enqueue(any())).thenReturn(true);

        var result = service.sendPickupDeadlineReminders();

        ArgumentCaptor<NotificationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(outboxService, times(2)).enqueue(eventCaptor.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(2);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(eventCaptor.getAllValues())
                    .extracting(NotificationRequestedEvent::aggregateId)
                    .containsExactly(101L, 102L);
            softly.assertThat(eventCaptor.getAllValues())
                    .extracting(NotificationRequestedEvent::idempotencyKey)
                    .containsExactly(
                            "USER:10:PICKUP_DEADLINE_REMINDER:ORDER:101",
                            "USER:10:PICKUP_DEADLINE_REMINDER:ORDER:102");
        });
    }
}
