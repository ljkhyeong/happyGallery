package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@UseCaseIT
class RefundExecutionServiceUseCaseIT {

    @Autowired RefundExecutionService refundExecutionService;
    @Autowired RefundRepository refundRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired Clock clock;
    @MockitoBean PaymentProvider paymentProvider;

    @DisplayName("외부 트랜잭션이 롤백되어도 환불 실패 이력은 커밋되어 남는다")
    @Test
    void keepsFailedRefundLog_whenOuterTransactionRollsBack() {
        refundRepository.deleteAllInBatch();

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = orderRepository.save(Order.forGuest(null, null, 55_000L, paidAt, paidAt.plusHours(24)));
        doThrow(new RuntimeException("PG timeout")).when(paymentProvider).refund(any(), anyLong());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            refundExecutionService.processOrderRefund(order.getId(), 55_000L);
            throw new RuntimeException("outer rollback");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("outer rollback");

        var refunds = refundRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refunds.get(0).getOrderId()).isEqualTo(order.getId());
            softly.assertThat(refunds.get(0).getStatus()).isEqualTo(RefundStatus.FAILED);
            softly.assertThat(refunds.get(0).getFailReason()).contains("PG timeout");
        });
    }
}
