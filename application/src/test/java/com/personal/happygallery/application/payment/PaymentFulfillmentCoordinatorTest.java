package com.personal.happygallery.application.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentFulfillmentCoordinatorTest {

    private static final String COMPENSATION_REASON = "PG 승인 후 도메인 생성에 실패했습니다.";

    private final PaymentConfirmFulfillmentTransactionService fulfillmentTransactionService =
            mock(PaymentConfirmFulfillmentTransactionService.class);
    private final PaymentFulfillmentCoordinator coordinator =
            new PaymentFulfillmentCoordinator(fulfillmentTransactionService);

    @DisplayName("유료 결제의 도메인 생성 실패는 확정 paymentKey와 사유로 보상 환불을 요청한 뒤 원래 예외를 다시 던진다")
    @Test
    void fulfillOrCompensate_paidFulfillmentFailure_requestsCompensationAndRethrows() {
        RuntimeException fulfillmentFailure = new IllegalStateException("fulfillment failed");
        when(fulfillmentTransactionService.fulfillAndConfirm(1L)).thenThrow(fulfillmentFailure);
        when(fulfillmentTransactionService.requestCompensationAfterFulfillmentFailure(
                1L, "confirmed-key", COMPENSATION_REASON)).thenReturn(true);

        assertThatThrownBy(() -> coordinator.fulfillOrCompensate(
                1L, "order-id", 10_000L, "confirmed-key", COMPENSATION_REASON))
                .isSameAs(fulfillmentFailure);

        verify(fulfillmentTransactionService).requestCompensationAfterFulfillmentFailure(
                1L, "confirmed-key", COMPENSATION_REASON);
        verify(fulfillmentTransactionService, never()).tryMarkZeroAmountFulfillmentFailed(
                anyLong(), anyString());
    }

    @DisplayName("보상 환불 요청 저장 실패는 원래 fulfillment 예외에 보조 원인으로 보존한다")
    @Test
    void fulfillOrCompensate_compensationPersistenceFailure_isSuppressedOnFulfillmentFailure() {
        RuntimeException fulfillmentFailure = new IllegalStateException("fulfillment failed");
        RuntimeException compensationFailure = new IllegalStateException("compensation save failed");
        when(fulfillmentTransactionService.fulfillAndConfirm(1L)).thenThrow(fulfillmentFailure);
        when(fulfillmentTransactionService.requestCompensationAfterFulfillmentFailure(
                1L, "confirmed-key", COMPENSATION_REASON)).thenThrow(compensationFailure);

        assertThatThrownBy(() -> coordinator.fulfillOrCompensate(
                1L, "order-id", 10_000L, "confirmed-key", COMPENSATION_REASON))
                .isSameAs(fulfillmentFailure)
                .satisfies(thrown -> assertThat(thrown.getSuppressed()).containsExactly(compensationFailure));
    }

    @DisplayName("0원 fulfillment 실패 상태 저장 오류는 원래 fulfillment 예외를 가리지 않는다")
    @Test
    void fulfillOrCompensate_zeroAmountFailurePersistenceError_preservesFulfillmentFailure() {
        RuntimeException fulfillmentFailure = new IllegalStateException("fulfillment failed");
        RuntimeException stateFailure = new IllegalStateException("state save failed");
        when(fulfillmentTransactionService.fulfillAndConfirm(1L)).thenThrow(fulfillmentFailure);
        when(fulfillmentTransactionService.tryMarkZeroAmountFulfillmentFailed(
                1L, "도메인 생성에 실패했습니다."))
                .thenThrow(stateFailure);

        assertThatThrownBy(() -> coordinator.fulfillOrCompensate(
                1L, "order-id", 0L, null, COMPENSATION_REASON))
                .isSameAs(fulfillmentFailure)
                .satisfies(thrown -> assertThat(thrown.getSuppressed()).containsExactly(stateFailure));

        verify(fulfillmentTransactionService, never()).requestCompensationAfterFulfillmentFailure(
                anyLong(), anyString(), anyString());
    }
}
