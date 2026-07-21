package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.payment.PaymentConfirmTransactionService.PgConfirmationRequired;
import com.personal.happygallery.application.payment.PaymentConfirmTransactionService.ReadyForFulfillment;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class PaymentConfirmTransactionServiceIT {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentConfirmTransactionService transactionService;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired UserStorePort userStorePort;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearPassData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("stale 결제 재선점 뒤 이전 실행권의 PG 결과는 현재 상태를 변경하지 않는다")
    @Test
    void resolveConfirmationStep_staleTakeover_fencesPreviousPgResult() {
        PreparedPass prepared = preparePass("payment-fence@example.com", "01012344321");
        ConfirmCommand command = prepared.command("payment-key");

        PgConfirmationRequired first = (PgConfirmationRequired)
                transactionService.resolveConfirmationStep(command);
        makeProcessingStale(first.attemptId());
        PgConfirmationRequired second = (PgConfirmationRequired)
                transactionService.resolveConfirmationStep(command);

        assertSoftly(softly -> {
            softly.assertThat(second.processingToken()).isNotEqualTo(first.processingToken());
            softly.assertThat(transactionService.tryRecordPgFailure(
                    first.attemptId(), first.processingToken(), "늦게 도착한 실패", true)).isFalse();
            softly.assertThat(transactionService.tryMarkApproved(
                    first.attemptId(), first.processingToken(), "confirmed-payment-key")).isFalse();
        });

        assertThat(transactionService.tryMarkApproved(
                second.attemptId(), second.processingToken(), "confirmed-payment-key")).isTrue();
        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt -> assertSoftly(softly -> {
                    softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
                    softly.assertThat(attempt.getConfirmedPaymentKey()).isEqualTo("confirmed-payment-key");
                    softly.assertThat(attempt.getProcessingToken()).isNull();
                }));
    }

    @DisplayName("새 실행권 실패 뒤 도착한 이전 PG 성공은 APPROVED로 화해한다")
    @Test
    void reconcileLatePgApproval_afterNewOwnerFailure_restoresExternalApproval() {
        PreparedPass prepared = preparePass("payment-reconcile@example.com", "01022223333");
        ConfirmCommand command = prepared.command("payment-key");

        PgConfirmationRequired first = (PgConfirmationRequired)
                transactionService.resolveConfirmationStep(command);
        makeProcessingStale(first.attemptId());
        PgConfirmationRequired second = (PgConfirmationRequired)
                transactionService.resolveConfirmationStep(command);
        assertThat(transactionService.tryRecordPgFailure(
                second.attemptId(), second.processingToken(), "새 실행권의 타임아웃", true)).isTrue();
        assertThatThrownBy(() -> transactionService.resolveAfterLostProcessingOwnership(command))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertSoftly(softly -> {
                        softly.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_CONFIRM_RETRYABLE);
                        softly.assertThat(exception.getMessage()).isEqualTo("새 실행권의 타임아웃");
                    });
                });

        ReadyForFulfillment reconciled = (ReadyForFulfillment)
                transactionService.reconcileLatePgApproval(command, "confirmed-payment-key");

        assertSoftly(softly -> {
            softly.assertThat(reconciled.attemptId()).isEqualTo(first.attemptId());
            softly.assertThat(reconciled.confirmedPaymentKey()).isEqualTo("confirmed-payment-key");
            softly.assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                    .hasValueSatisfying(attempt -> {
                        softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
                        softly.assertThat(attempt.getConfirmedPaymentKey()).isEqualTo("confirmed-payment-key");
                        softly.assertThat(attempt.getProcessingToken()).isNull();
                    });
        });
    }

    @DisplayName("결제 confirm은 기존 DB 트랜잭션 안에서 호출할 수 없다")
    @Test
    void confirm_insideExistingTransaction_isRejectedBeforeExecution() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> assertThatThrownBy(() -> confirmUseCase.confirm(
                ConfirmCommand.customerRequest(
                        "payment-key", "order-id", 10_000L, AuthContext.guest(), "status-token")))
                .isInstanceOf(IllegalTransactionStateException.class));
    }

    private PreparedPass preparePass(String email, String phone) {
        User user = userStorePort.save(new User(email, "hashed", "회원", phone));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS, new PassPayload(user.getId()), auth));
        return new PreparedPass(prepared.orderId(), prepared.amount(), auth);
    }

    private void makeProcessingStale(Long attemptId) {
        jdbcTemplate.update(
                "UPDATE payment_attempt SET processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusMinutes(2), attemptId);
    }

    private record PreparedPass(String orderId, long amount, AuthContext auth) {
        ConfirmCommand command(String paymentKey) {
            return ConfirmCommand.customerRequest(paymentKey, orderId, amount, auth, null);
        }
    }
}
