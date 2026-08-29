package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentWebhookReceiptPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPaymentWebhookServiceTest {

    private final PaymentAttemptReaderPort attemptReader = mock(PaymentAttemptReaderPort.class);
    private final PaymentWebhookReceiptPort receiptPort = mock(PaymentWebhookReceiptPort.class);
    private final PaymentWebhookReceiptTransactionService receiptTransactionService =
            mock(PaymentWebhookReceiptTransactionService.class);
    private final PaymentReconciliationAdminUseCase reconciliationUseCase =
            mock(PaymentReconciliationAdminUseCase.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneOffset.UTC);
    private DefaultPaymentWebhookService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentWebhookService(
                attemptReader,
                receiptPort,
                receiptTransactionService,
                reconciliationUseCase,
                clock);
    }

    @DisplayName("결제 상태 변경 웹훅은 알려진 주문의 전송 식별자를 한 번 저장한다")
    @Test
    void receive_knownPaymentStatusChanged_registersReceipt() {
        PaymentAttempt attempt = mock(PaymentAttempt.class);
        when(attempt.getId()).thenReturn(11L);
        when(attemptReader.findByOrderIdExternal("order-1")).thenReturn(Optional.of(attempt));

        service.receive("transmission-1", "PAYMENT_STATUS_CHANGED", "order-1");

        verify(receiptPort).insertIfAbsent(
                "transmission-1",
                11L,
                "PAYMENT_STATUS_CHANGED",
                LocalDateTime.now(clock));
    }

    @DisplayName("결제 상태 변경이 아닌 웹훅은 저장하지 않는다")
    @Test
    void receive_otherEvent_ignoresReceipt() {
        service.receive("transmission-1", "PAYOUT_STATUS_CHANGED", "order-1");

        verify(attemptReader, never()).findByOrderIdExternal("order-1");
        verify(receiptPort, never()).insertIfAbsent(
                "transmission-1", 11L, "PAYOUT_STATUS_CHANGED", LocalDateTime.now(clock));
    }

    @DisplayName("대기 중인 결제 웹훅은 기존 PG 대사를 실행한 뒤 완료 처리한다")
    @Test
    void processPendingReceipts_reconcilesAndCompletesClaimedReceipt() {
        LocalDateTime staleBefore = LocalDateTime.now(clock).minusMinutes(1);
        when(receiptPort.findPendingIds(staleBefore, 20)).thenReturn(List.of(31L));
        when(receiptTransactionService.claim(31L, staleBefore)).thenReturn(Optional.of(11L));

        var result = service.processPendingReceipts();

        assertThat(result.successCount()).isEqualTo(1);
        verify(reconciliationUseCase).reconcile(11L);
        verify(receiptTransactionService).complete(31L);
    }
}
