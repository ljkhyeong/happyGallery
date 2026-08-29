package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.monitoring.AppMetrics;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.PgConfirmationRequired;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.ReadyForFulfillment;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.ZeroAmountApprovalRequired;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPaymentConfirmServiceTest {

    private static final ConfirmCommand COMMAND = ConfirmCommand.trustedRecovery(
            "payment-key", "order-id", 10_000L, AuthContext.guest());

    @Mock PaymentPort paymentPort;
    @Mock PaymentConfirmClaimTransactionService claimTransactionService;
    @Mock PaymentConfirmFulfillmentTransactionService fulfillmentTransactionService;
    @Mock AppMetrics appMetrics;
    @InjectMocks DefaultPaymentConfirmService service;

    @DisplayName("수동 대사 상태 전이는 운영 카운터를 기록한 뒤 결제 실패를 반환한다")
    @Test
    void confirm_reconciliationRequired_recordsOperationalSignalBeforeFailure() {
        HappyGalleryException failure = new HappyGalleryException(
                ErrorCode.PAYMENT_FAILED, "수동 대사가 필요합니다.");
        when(claimTransactionService.resolveConfirmationStep(COMMAND))
                .thenReturn(new PaymentConfirmClaimTransactionService.ConfirmationRejected(1L, failure));

        assertThatThrownBy(() -> service.confirm(COMMAND)).isSameAs(failure);

        verify(appMetrics).incrementPaymentConfirmReconciliationRequired();
        verifyNoInteractions(paymentPort);
    }

    @DisplayName("0원 결제 승인 상태 저장 실패는 보상 환불을 요청하지 않는다")
    @Test
    void confirm_zeroAmountApprovalPersistenceFailure_doesNotRequestCompensation() {
        ZeroAmountApprovalRequired required = new ZeroAmountApprovalRequired(1L, "order-id", "token");
        RuntimeException approvalFailure = new IllegalStateException("approval save failed");
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(claimTransactionService.tryMarkApproved(1L, "token", null, null)).thenThrow(approvalFailure);

        assertThatThrownBy(() -> service.confirm(COMMAND)).isSameAs(approvalFailure);

        verifyNoInteractions(paymentPort);
        verify(fulfillmentTransactionService, never()).requestCompensationForUnpersistedApproval(
                anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(fulfillmentTransactionService, never()).requestCompensationAfterFulfillmentFailure(
                anyLong(), anyString(), anyString());
    }

    @DisplayName("PG 승인 상태 저장 실패는 같은 실행권으로 보상 환불을 요청한다")
    @Test
    void confirm_paidApprovalPersistenceFailure_requestsCompensationWithProcessingToken() {
        PgConfirmationRequired required = paidConfirmationRequired();
        RuntimeException approvalFailure = new IllegalStateException("approval save failed");
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.success("confirmed-key", "CARD", "approved-at"));
        when(claimTransactionService.tryMarkApproved(1L, "token", "confirmed-key", "CARD"))
                .thenThrow(approvalFailure);
        when(fulfillmentTransactionService.requestCompensationForUnpersistedApproval(
                1L, "token", "confirmed-key", "CARD", "PG 승인 후 결제 상태 저장에 실패했습니다."))
                .thenReturn(true);

        assertThatThrownBy(() -> service.confirm(COMMAND)).isSameAs(approvalFailure);

        verify(fulfillmentTransactionService).requestCompensationForUnpersistedApproval(
                1L, "token", "confirmed-key", "CARD", "PG 승인 후 결제 상태 저장에 실패했습니다.");
    }

    @DisplayName("보상 환불 요청 저장 실패는 원래 예외에 보조 원인으로 보존한다")
    @Test
    void confirm_compensationPersistenceFailure_isSuppressedOnOriginalFailure() {
        PgConfirmationRequired required = paidConfirmationRequired();
        RuntimeException approvalFailure = new IllegalStateException("approval save failed");
        RuntimeException compensationFailure = new IllegalStateException("compensation save failed");
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.success("confirmed-key", "CARD", "approved-at"));
        when(claimTransactionService.tryMarkApproved(1L, "token", "confirmed-key", "CARD"))
                .thenThrow(approvalFailure);
        when(fulfillmentTransactionService.requestCompensationForUnpersistedApproval(
                1L, "token", "confirmed-key", "CARD", "PG 승인 후 결제 상태 저장에 실패했습니다."))
                .thenThrow(compensationFailure);

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isSameAs(approvalFailure)
                .satisfies(thrown -> assertThat(thrown.getSuppressed()).containsExactly(compensationFailure));
    }

    @DisplayName("PaymentPort 계약 밖 호출 예외는 PG 실패로 숨기지 않고 전파한다")
    @Test
    void confirm_paymentPortException_propagatesWithoutRecordingPgFailure() {
        PgConfirmationRequired required = paidConfirmationRequired();
        RuntimeException unexpected = new NullPointerException("adapter bug");
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenThrow(unexpected);

        assertThatThrownBy(() -> service.confirm(COMMAND)).isSameAs(unexpected);

        verify(claimTransactionService, never()).tryRecordPgFailure(
                anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @DisplayName("늦게 도착한 PG 실패는 새 실행권이 완료한 결과를 반환한다")
    @Test
    void confirm_stalePgFailure_returnsLatestCompletedResult() {
        PgConfirmationRequired required = paidConfirmationRequired();
        ConfirmResult completed = new ConfirmResult(PaymentContext.ORDER, 10L, null, false);
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.failure("PG 거절"));
        when(claimTransactionService.tryRecordPgFailure(1L, "token", "PG 거절", false, false))
                .thenReturn(false);
        when(claimTransactionService.resolveAfterLostProcessingOwnership(COMMAND))
                .thenReturn(new PaymentConfirmClaimTransactionService.Completed(completed));

        assertThat(service.confirm(COMMAND)).isEqualTo(completed);
    }

    @DisplayName("PG 응답 식별자 불일치는 즉시 수동 대사 상태로 저장한다")
    @Test
    void confirm_pgIdentityMismatch_recordsReconciliationRequired() {
        PgConfirmationRequired required = paidConfirmationRequired();
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.reconciliationRequired("PG 응답 식별자 불일치"));
        when(claimTransactionService.tryRecordPgReconciliationRequired(
                1L, "token", "PG 응답 식별자 불일치")).thenReturn(true);

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_RECONCILIATION_REQUIRED));

        verify(appMetrics).incrementPaymentConfirmReconciliationRequired();
        verify(claimTransactionService, never()).tryRecordPgFailure(
                anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @DisplayName("이전 PG 호출이 있을 수 있는 최종 실패는 혜택을 풀지 않고 대상 상태로 격리한다")
    @Test
    void confirm_finalFailureAfterPriorCall_recordsReconciliationRequired() {
        PgConfirmationRequired required = new PgConfirmationRequired(
                1L, "order-id", 10_000L, "payment-key", "token", true);
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.failure("PG 최종 거절"));
        when(claimTransactionService.tryRecordPgReconciliationRequired(
                1L, "token", "PG 최종 거절")).thenReturn(true);

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_RECONCILIATION_REQUIRED));

        verify(appMetrics).incrementPaymentConfirmReconciliationRequired();
        verify(claimTransactionService, never()).tryRecordPgFailure(
                anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @DisplayName("늦게 도착한 PG 식별자 불일치 결과는 최신 실행권이 완료한 결과를 반환한다")
    @Test
    void confirm_stalePgIdentityMismatch_returnsLatestCompletedResult() {
        PgConfirmationRequired required = paidConfirmationRequired();
        ConfirmResult completed = new ConfirmResult(PaymentContext.ORDER, 10L, null, false);
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.reconciliationRequired("PG 응답 식별자 불일치"));
        when(claimTransactionService.tryRecordPgReconciliationRequired(
                1L, "token", "PG 응답 식별자 불일치")).thenReturn(false);
        when(claimTransactionService.resolveAfterLostProcessingOwnership(COMMAND))
                .thenReturn(new PaymentConfirmClaimTransactionService.Completed(completed));

        assertThat(service.confirm(COMMAND)).isEqualTo(completed);

        verify(appMetrics, never()).incrementPaymentConfirmReconciliationRequired();
    }

    @DisplayName("늦게 도착한 PG 성공은 최신 로컬 실패와 화해한 뒤 fulfillment를 이어간다")
    @Test
    void confirm_stalePgSuccess_reconcilesAndFulfills() {
        PgConfirmationRequired required = paidConfirmationRequired();
        ReadyForFulfillment ready = new ReadyForFulfillment(
                1L, "order-id", 10_000L, "confirmed-key");
        ConfirmResult completed = new ConfirmResult(PaymentContext.ORDER, 10L, null, false);
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(required);
        when(paymentPort.confirm("payment-key", "order-id", 10_000L, "order-id"))
                .thenReturn(PaymentConfirmResult.success("confirmed-key", "CARD", "approved-at"));
        when(claimTransactionService.tryMarkApproved(1L, "token", "confirmed-key", "CARD"))
                .thenReturn(false);
        when(claimTransactionService.reconcileLatePgApproval(COMMAND, "confirmed-key", "CARD"))
                .thenReturn(ready);
        when(fulfillmentTransactionService.fulfillAndConfirm(1L)).thenReturn(completed);

        assertThat(service.confirm(COMMAND)).isEqualTo(completed);

        verify(claimTransactionService).reconcileLatePgApproval(COMMAND, "confirmed-key", "CARD");
    }

    @DisplayName("0원 fulfillment 실패 상태 저장 오류는 원래 fulfillment 예외를 가리지 않는다")
    @Test
    void confirm_zeroAmountFailurePersistenceError_preservesFulfillmentFailure() {
        ReadyForFulfillment ready = new ReadyForFulfillment(1L, "order-id", 0L, null);
        RuntimeException fulfillmentFailure = new IllegalStateException("fulfillment failed");
        RuntimeException stateFailure = new IllegalStateException("state save failed");
        when(claimTransactionService.resolveConfirmationStep(COMMAND)).thenReturn(ready);
        when(fulfillmentTransactionService.fulfillAndConfirm(1L)).thenThrow(fulfillmentFailure);
        when(fulfillmentTransactionService.tryMarkZeroAmountFulfillmentFailed(
                1L, "도메인 생성에 실패했습니다."))
                .thenThrow(stateFailure);

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isSameAs(fulfillmentFailure)
                .satisfies(thrown -> assertThat(thrown.getSuppressed()).containsExactly(stateFailure));

        verify(fulfillmentTransactionService, never()).requestCompensationAfterFulfillmentFailure(
                anyLong(), anyString(), anyString());
    }

    private PgConfirmationRequired paidConfirmationRequired() {
        return new PgConfirmationRequired(1L, "order-id", 10_000L, "payment-key", "token");
    }
}
