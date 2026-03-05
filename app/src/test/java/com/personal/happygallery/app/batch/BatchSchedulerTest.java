package com.personal.happygallery.app.batch;

import com.personal.happygallery.app.booking.BookingReminderBatchService;
import com.personal.happygallery.app.order.OrderAutoRefundBatchService;
import com.personal.happygallery.app.order.PickupExpireBatchService;
import com.personal.happygallery.app.pass.PassExpiryBatchService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchSchedulerTest {

    private final OrderAutoRefundBatchService orderAutoRefundBatchService = mock(OrderAutoRefundBatchService.class);
    private final PickupExpireBatchService pickupExpireBatchService = mock(PickupExpireBatchService.class);
    private final PassExpiryBatchService passExpiryBatchService = mock(PassExpiryBatchService.class);
    private final BookingReminderBatchService bookingReminderBatchService = mock(BookingReminderBatchService.class);

    private final BatchScheduler batchScheduler = new BatchScheduler(
            orderAutoRefundBatchService,
            pickupExpireBatchService,
            passExpiryBatchService,
            bookingReminderBatchService);

    @Test
    void runOrderAutoRefund_delegatesAndReturnsResult() {
        BatchResult expected = BatchResult.successOnly(2);
        when(orderAutoRefundBatchService.autoRefundExpired()).thenReturn(expected);

        BatchResult actual = batchScheduler.runOrderAutoRefund();

        assertThat(actual).isEqualTo(expected);
        verify(orderAutoRefundBatchService).autoRefundExpired();
    }

    @Test
    void runPickupExpire_propagatesException() {
        RuntimeException failure = new RuntimeException("pickup failed");
        when(pickupExpireBatchService.expirePickups()).thenThrow(failure);

        assertThatThrownBy(() -> batchScheduler.runPickupExpire())
                .isSameAs(failure);
    }
}
