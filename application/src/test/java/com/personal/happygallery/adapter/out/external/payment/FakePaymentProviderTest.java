package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class FakePaymentProviderTest {

    @DisplayName("local 결제 확정은 성공 결과와 fake PG 참조값을 반환한다")
    @Test
    void confirm_returnsSuccessWithFakePgReference() {
        FakePaymentProvider paymentProvider = new FakePaymentProvider();

        PaymentConfirmResult result = paymentProvider.confirm("payment-key", "order-id", 5_000L);

        assertSoftly(softly -> {
            softly.assertThat(result.success()).isTrue();
            softly.assertThat(result.pgRef()).startsWith("FAKE-PG-");
            softly.assertThat(result.method()).isEqualTo("FAKE_PG");
            softly.assertThat(result.approvedAt()).isNotBlank();
            softly.assertThat(result.failReason()).isNull();
        });
    }

    @DisplayName("local 환불 실패 훅이 arm되면 다음 환불 1회만 실패한다")
    @Test
    void refund_failsOnlyOnce_whenLocalFailureIsArmed() {
        LocalRefundFailureScript localRefundFailureScript = new LocalRefundFailureScript();
        FakePaymentProvider paymentProvider = new FakePaymentProvider(localRefundFailureScript);

        localRefundFailureScript.armNextFailure("PG 강제 실패");

        RefundResult failed = paymentProvider.refund("PG-REF-1", 5_000L);
        RefundResult succeeded = paymentProvider.refund("PG-REF-1", 5_000L);

        assertSoftly(softly -> {
            softly.assertThat(failed.success()).isFalse();
            softly.assertThat(failed.failReason()).isEqualTo("PG 강제 실패");
            softly.assertThat(succeeded.success()).isTrue();
            softly.assertThat(succeeded.pgRef()).startsWith("FAKE-REFUND-");
        });
    }
}
