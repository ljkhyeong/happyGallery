package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPaymentAttemptExpiryBatchServiceTest {

    @DisplayName("만료 결제 배치는 앞쪽 실패가 있어도 200건 뒤 후보까지 ID 키셋으로 처리한다")
    @Test
    void expirePendingAttempts_pagesBeyondTwoHundredAndIsolatesEarlyFailure() {
        PaymentAttemptReaderPort attemptReader = mock(PaymentAttemptReaderPort.class);
        PaymentAttemptExpiryProcessor expiryProcessor = mock(PaymentAttemptExpiryProcessor.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneOffset.UTC);
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                clock.instant().minus(DefaultPaymentAttemptExpiryBatchService.PREPARE_TTL),
                ZoneOffset.UTC);
        List<Long> firstPage = ids(1, 100);
        List<Long> secondPage = ids(101, 200);
        List<Long> thirdPage = ids(201, 205);
        when(attemptReader.findExpiredPendingIdsAfterId(cutoff, 0L, 100))
                .thenReturn(firstPage);
        when(attemptReader.findExpiredPendingIdsAfterId(cutoff, 100L, 100))
                .thenReturn(secondPage);
        when(attemptReader.findExpiredPendingIdsAfterId(cutoff, 200L, 100))
                .thenReturn(thirdPage);
        when(attemptReader.findExpiredPendingIdsAfterId(cutoff, 205L, 100))
                .thenReturn(List.of());
        when(expiryProcessor.expire(anyLong(), eq(cutoff))).thenReturn(true);
        when(expiryProcessor.expire(1L, cutoff))
                .thenThrow(new IllegalStateException("첫 후보 처리 실패"));
        DefaultPaymentAttemptExpiryBatchService service =
                new DefaultPaymentAttemptExpiryBatchService(
                        attemptReader, expiryProcessor, clock);

        BatchResult result = service.expirePendingAttempts();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(204);
            softly.assertThat(result.failureCount()).isOne();
            softly.assertThat(result.failureReasons())
                    .containsEntry("IllegalStateException", 1);
        });
        verify(expiryProcessor).expire(205L, cutoff);
        verify(attemptReader).findExpiredPendingIdsAfterId(cutoff, 205L, 100);
    }

    private static List<Long> ids(long startInclusive, long endInclusive) {
        return LongStream.rangeClosed(startInclusive, endInclusive)
                .boxed()
                .toList();
    }
}
