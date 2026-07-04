package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefundRetryUseCaseTest {

    @DisplayName("FAILED 환불만 재시도 실행 서비스로 위임한다")
    @Test
    void retry_failedRefund_delegatesExecution() {
        RefundPort refundPort = mock(RefundPort.class);
        RefundExecutionService refundExecutionService = mock(RefundExecutionService.class);
        DefaultRefundRetryService service = new DefaultRefundRetryService(refundPort, refundExecutionService);

        service.retry(1L);

        verify(refundExecutionService).retryRefund(1L);
        verifyNoInteractions(refundPort);
    }

    @DisplayName("실패 환불 목록은 FAILED 상태 조회 결과를 반환한다")
    @Test
    void listFailed_returnsFailedRefunds() {
        Refund failedRefund = failedRefund();
        RefundPort refundPort = mock(RefundPort.class);
        DefaultRefundRetryService service = new DefaultRefundRetryService(
                refundPort,
                mock(RefundExecutionService.class));
        when(refundPort.findByStatus(RefundStatus.FAILED))
                .thenReturn(List.of(failedRefund));

        List<Refund> result = service.listFailed();

        assertSoftly(softly -> softly.assertThat(result).containsExactly(failedRefund));
    }

    private static Refund failedRefund() {
        Refund refund = Refund.forOrder(10L, 5_000L, "payment-key");
        refund.markFailed("PG timeout");
        return refund;
    }
}
