package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentStrategyRegistrationTest {

    @DisplayName("같은 결제 컨텍스트의 준비 또는 확정 전략이 중복되면 서비스 생성을 거부한다")
    @Test
    void constructors_duplicateContext_throwIllegalStateException() {
        PaymentPreparer firstPreparer = mock(PaymentPreparer.class);
        PaymentPreparer secondPreparer = mock(PaymentPreparer.class);
        when(firstPreparer.context()).thenReturn(PaymentContext.ORDER);
        when(secondPreparer.context()).thenReturn(PaymentContext.ORDER);

        PaymentFulfiller firstFulfiller = mock(PaymentFulfiller.class);
        PaymentFulfiller secondFulfiller = mock(PaymentFulfiller.class);
        when(firstFulfiller.context()).thenReturn(PaymentContext.BOOKING);
        when(secondFulfiller.context()).thenReturn(PaymentContext.BOOKING);

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new DefaultPaymentPrepareService(
                            List.of(firstPreparer, secondPreparer),
                            mock(PaymentAttemptStorePort.class),
                            mock(ObjectMapper.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("결제 준비 전략")
                    .hasMessageContaining("ORDER");

            softly.assertThatThrownBy(() -> new DefaultPaymentConfirmService(
                            mock(PaymentAttemptReaderPort.class),
                            mock(PaymentAttemptStorePort.class),
                            mock(PaymentPort.class),
                            List.of(firstFulfiller, secondFulfiller),
                            mock(ObjectMapper.class),
                            mock(Clock.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("결제 확정 전략")
                    .hasMessageContaining("BOOKING");
        });
    }
}
