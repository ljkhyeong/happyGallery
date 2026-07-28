package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.token.GuestTokenProperties;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestPaymentVerificationServiceTest {

    @DisplayName("결제 휴대폰 인증 증거는 발급한 결제 시도와 컨텍스트 및 전화번호에서만 유효하다")
    @Test
    void proof_isBoundToPaymentContextOrderIdAndPhone() {
        AtomicInteger consumed = new AtomicInteger();
        PhoneOwnershipVerificationUseCase ownershipVerification =
                (phone, code, purpose) -> consumed.incrementAndGet();
        GuestPaymentVerificationService service = new GuestPaymentVerificationService(
                ownershipVerification,
                new GuestTokenProperties("active-secret-must-be-at-least-32-characters", "", 720, 24));
        String orderId = "1e3f9d80-c482-4aa6-86c8-c0f8d0951463";
        String phone = "01012345678";

        String proof = service.consumeAndIssue(
                PaymentContext.ORDER, orderId, phone, "123456");

        assertThatCode(() -> service.requireValid(PaymentContext.ORDER, orderId, phone, proof))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireValid(
                PaymentContext.ORDER, "35aa91de-98a6-43ed-a916-06181d9f0cf0", phone, proof))
                .isInstanceOf(HappyGalleryException.class);
        assertThatThrownBy(() -> service.requireValid(PaymentContext.BOOKING, orderId, phone, proof))
                .isInstanceOf(HappyGalleryException.class);
        assertThatThrownBy(() -> service.requireValid(
                PaymentContext.ORDER, orderId, "01087654321", proof))
                .isInstanceOf(HappyGalleryException.class);
        assertThat(consumed).hasValue(1);
        assertThat(proof).doesNotContain(phone, "123456");
    }
}
