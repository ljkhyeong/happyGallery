package com.personal.happygallery.application.payment;

import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class PaymentStatusRecoveryUseCaseIT {

    private static final String PHONE = "01012345678";

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentStatusRecoveryUseCase recoveryUseCase;
    @Autowired PaymentStatusQueryUseCase statusQueryUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired PaymentAttemptStorePort attemptStore;
    @Autowired RefundRepository refundRepository;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired PhoneVerificationStorePort verificationStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("휴대폰 인증으로 유실한 비회원 결제 목록과 공통 상태 조회 토큰을 복구한다")
    @Test
    void recover_replacesTokensAndReturnsAllPayments() {
        Product product = productStorePort.save(readyStockProduct("복구 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 2));
        PaymentPrepareUseCase.PrepareResult first = prepare(product, PHONE);
        PaymentPrepareUseCase.PrepareResult second = prepare(product, PHONE);
        PaymentPrepareUseCase.PrepareResult third = prepare(product, PHONE);
        markCompensationRequested(first);
        markCompensationSucceeded(second);
        String verificationCode = saveVerification(
                PHONE, "123456", PhoneVerificationPurpose.GUEST_PAYMENT_STATUS_RECOVERY);

        PaymentStatusRecoveryUseCase.RecoveryResult recovered =
                recoveryUseCase.recover(PHONE, verificationCode);

        assertSoftly(softly -> {
            softly.assertThat(recovered.statusToken()).isNotBlank();
            softly.assertThat(recovered.expiresAt()).isAfter(clock.instant());
            softly.assertThat(recovered.payments())
                    .extracting(PaymentStatusRecoveryUseCase.RecoveredPayment::orderId)
                    .containsExactly(first.orderId(), second.orderId(), third.orderId());
            softly.assertThat(recovered.payments())
                    .allSatisfy(payment -> {
                        softly.assertThat(payment.context()).isEqualTo(PaymentContext.ORDER);
                        softly.assertThat(payment.amount()).isEqualTo(29_000L);
                    });
            softly.assertThat(recovered.payments().getFirst().status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.REFUNDING);
            softly.assertThat(recovered.payments().get(1).status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.REFUNDED);
            softly.assertThat(recovered.payments().getLast().status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.READY);
            softly.assertThat(statusQueryUseCase.getStatus(
                            first.orderId(), AuthContext.guest(), recovered.statusToken()).status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.REFUNDING);
            softly.assertThat(statusQueryUseCase.getStatus(
                            second.orderId(), AuthContext.guest(), recovered.statusToken()).status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.REFUNDED);
            softly.assertThat(statusQueryUseCase.getStatus(
                            third.orderId(), AuthContext.guest(), recovered.statusToken()).status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.READY);
        });
        assertThatThrownBy(() -> statusQueryUseCase.getStatus(
                first.orderId(), AuthContext.guest(), first.statusToken()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> statusQueryUseCase.getStatus(
                second.orderId(), AuthContext.guest(), second.statusToken()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> statusQueryUseCase.getStatus(
                third.orderId(), AuthContext.guest(), third.statusToken()))
                .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("인증한 휴대폰과 결제 소유자가 다르면 결제 미존재와 같은 응답으로 거절한다")
    @Test
    void recover_differentPhoneRejectedAsNotFound() {
        Product product = productStorePort.save(readyStockProduct("타인 결제 복구 상품", 19_000L));
        inventoryStorePort.save(inventory(product, 1));
        prepare(product, PHONE);
        String otherPhone = "01087654321";
        String verificationCode = saveVerification(
                otherPhone, "654321", PhoneVerificationPurpose.GUEST_PAYMENT_STATUS_RECOVERY);

        assertThatThrownBy(() -> recoveryUseCase.recover(otherPhone, verificationCode))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("결제");
        assertThatThrownBy(() -> recoveryUseCase.recover(otherPhone, verificationCode))
                .isInstanceOf(PhoneVerificationFailedException.class);
    }

    private PaymentPrepareUseCase.PrepareResult prepare(Product product, String phone) {
        String verificationCode = saveVerification(
                phone, "111111", PhoneVerificationPurpose.GUEST_ORDER);
        return prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        null, phone, verificationCode, "비회원",
                        List.of(new OrderItemRef(product.getId(), 1)),
                        false,
                        FulfillmentType.PICKUP,
                        null,
                        null,
                        false,
                        acceptedPolicies()),
                AuthContext.guest()));
    }

    private Refund markCompensationRequested(PaymentPrepareUseCase.PrepareResult prepared) {
        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        String processingToken = attempt.startProcessing(prepared.amount(), "compensation-key", now);
        attempt.markApproved(processingToken, "compensation-key", now);
        attempt.markCompensationRequested("주문 생성 실패");
        attemptStore.save(attempt);
        return refundRepository.save(Refund.forPaymentAttempt(
                attempt.getId(), attempt.getAmount(), attempt.getConfirmedPaymentKey()));
    }

    private void markCompensationSucceeded(PaymentPrepareUseCase.PrepareResult prepared) {
        Refund refund = markCompensationRequested(prepared);
        LocalDateTime now = LocalDateTime.now(clock);
        String processingToken = refund.startProcessing(now, now.minusMinutes(1));
        refund.markSucceeded(processingToken, "refund-transaction-key", now);
        refundRepository.save(refund);
    }

    private String saveVerification(
            String phone,
            String code,
            PhoneVerificationPurpose purpose) {
        PhoneVerification verification = new PhoneVerification(
                phone, code, purpose,
                LocalDateTime.now(clock).plusMinutes(5));
        verification.markDelivered();
        verificationStorePort.save(verification);
        return code;
    }
}
