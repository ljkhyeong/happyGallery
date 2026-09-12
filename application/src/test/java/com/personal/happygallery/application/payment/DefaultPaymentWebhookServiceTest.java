package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.PaymentWebhookReceiptPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        when(reconciliationUseCase.reconcile(11L)).thenReturn(
                new PaymentReconciliationAdminUseCase.ReconciliationResult(
                        11L, PaymentAttemptStatus.CONFIRMED, 41L, "결제 확정", false));

        var result = service.processPendingReceipts();

        assertThat(result.successCount()).isEqualTo(1);
        verify(reconciliationUseCase).reconcile(11L);
        verify(receiptTransactionService).complete(31L);
    }

    @DisplayName("PG 조회 장애는 웹훅을 완료하지 않고 운영자 확인 응답만 처리 완료로 기록한다")
    @ParameterizedTest
    @EnumSource(value = PaymentLookupResult.Status.class, names = {"UNAVAILABLE", "REVIEW_REQUIRED"})
    void processPendingReceipts_distinguishesLookupFailureFromManualReview(PaymentLookupResult.Status status) {
        PaymentPort paymentPort = mock(PaymentPort.class);
        PaymentReconciliationTransactionService reconciliationTransactions =
                mock(PaymentReconciliationTransactionService.class);
        var reconciler = new DefaultPaymentReconciliationAdminService(
                attemptReader, paymentPort, reconciliationTransactions,
                mock(PaymentConfirmFulfillmentTransactionService.class));
        service = new DefaultPaymentWebhookService(
                attemptReader, receiptPort, receiptTransactionService, reconciler, clock);
        LocalDateTime staleBefore = LocalDateTime.now(clock).minusMinutes(1);
        when(receiptPort.findPendingIds(staleBefore, 20)).thenReturn(List.of(31L));
        when(receiptTransactionService.claim(31L, staleBefore)).thenReturn(Optional.of(11L));
        when(reconciliationTransactions.prepareLookup(11L)).thenReturn(
                new PaymentReconciliationTransactionService.LookupRequest(11L, "order-1", "payment-key", 1000L));
        when(paymentPort.lookupByOrderId("order-1")).thenReturn(
                new PaymentLookupResult(status, null, "order-1", 0L, null, "조회 결과"));

        var result = service.processPendingReceipts();

        boolean unavailable = status == PaymentLookupResult.Status.UNAVAILABLE;
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(unavailable ? 0 : 1);
            softly.assertThat(result.failureCount()).isEqualTo(unavailable ? 1 : 0);
        });
        verify(receiptTransactionService, times(unavailable ? 0 : 1)).complete(31L);
    }
}
