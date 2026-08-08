package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.RefundTransactionService.RefundCall;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Refund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundDispatcherTest {

    @DisplayName("PG 취소액이 없는 환불은 결제사를 호출하지 않고 로컬 성공 처리한다")
    @Test
    void dispatch_localOnlyRefund_skipsPaymentProvider() {
        PaymentPort paymentPort = mock(PaymentPort.class);
        RefundTransactionService transactionService = mock(RefundTransactionService.class);
        Refund expected = mock(Refund.class);
        RefundCall.LocalOnlyRequired refundCall =
                new RefundCall.LocalOnlyRequired(7L, "processing-token");
        when(transactionService.claimRefundCall(7L)).thenReturn(refundCall);
        when(transactionService.markLocallySucceeded(7L, "processing-token"))
                .thenReturn(expected);

        Refund actual = new RefundDispatcher(paymentPort, transactionService)
                .dispatch(7L, "로컬 환불 단위 테스트");

        assertThat(actual).isSameAs(expected);
        verify(paymentPort, never()).refund(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
        verify(transactionService).markLocallySucceeded(7L, "processing-token");
    }

    @DisplayName("PG 구현이 잘못된 성공 결과를 만들면 성공 저장 대신 상태 확인 대상으로 격리한다")
    @Test
    void dispatch_malformedSuccessResult_requiresReconciliation() {
        PaymentPort paymentPort = mock(PaymentPort.class);
        RefundTransactionService transactionService = mock(RefundTransactionService.class);
        Refund expected = mock(Refund.class);
        RefundCall.CancelRequired refundCall = new RefundCall.CancelRequired(
                7L, "payment-key", 10_000L, "idempotency-key", "processing-token");
        when(transactionService.claimRefundCall(7L)).thenReturn(refundCall);
        when(paymentPort.refund("payment-key", 10_000L, "idempotency-key"))
                .thenAnswer(invocation -> RefundResult.success(" "));
        when(transactionService.markReconciliationRequired(
                7L, "processing-token", "PG 호출 결과를 확인할 수 없습니다."))
                .thenReturn(expected);

        Refund actual = new RefundDispatcher(paymentPort, transactionService)
                .dispatch(7L, "단위 테스트");

        assertThat(actual).isSameAs(expected);
        verify(transactionService, never()).markSucceeded(7L, "processing-token", " ");
        verify(transactionService).markReconciliationRequired(
                7L, "processing-token", "PG 호출 결과를 확인할 수 없습니다.");
    }
}
