package com.personal.happygallery.infra.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakePaymentProviderTest {

    @DisplayName("local 환불 실패 훅이 arm되면 다음 환불 1회만 실패한다")
    @Test
    void refund_failsOnlyOnce_whenLocalFailureIsArmed() {
        LocalRefundFailureScript localRefundFailureScript = new LocalRefundFailureScript();
        FakePaymentProvider paymentProvider = new FakePaymentProvider(localRefundFailureScript);

        localRefundFailureScript.armNextFailure("PG 강제 실패");

        RefundResult failed = paymentProvider.refund("PG-REF-1", 5_000L);
        RefundResult succeeded = paymentProvider.refund("PG-REF-1", 5_000L);

        assertThat(failed.success()).isFalse();
        assertThat(failed.failReason()).isEqualTo("PG 강제 실패");
        assertThat(succeeded.success()).isTrue();
        assertThat(succeeded.pgRef()).startsWith("FAKE-REFUND-");
    }
}
