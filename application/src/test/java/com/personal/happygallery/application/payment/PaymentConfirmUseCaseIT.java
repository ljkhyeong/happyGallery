package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class PaymentConfirmUseCaseIT {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired OrderReaderPort orderReader;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("confirm은 PG 성공 후 주문을 저장하고 결제 시도를 확정한다")
    @Test
    void confirm_success_storesOrderAndConfirmsAttempt() {
        User user = userStorePort.save(new User("payment-confirm@example.com", "hashed", "회원", "01056785678"));
        Product product = productStorePort.save(readyStockProduct("확정 상품", 31_000L));
        inventoryStorePort.save(inventory(product, 5));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand("payment-key-confirm", prepared.orderId(), prepared.amount(), auth));

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        var order = orderReader.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.context()).isEqualTo(PaymentContext.ORDER);
            softly.assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(order.getTotalAmount()).isEqualTo(31_000L);
            softly.assertThat(order.getPaymentKey()).startsWith("FAKE-PG-");
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(attempt.getPaymentKey()).isEqualTo("payment-key-confirm");
            softly.assertThat(attempt.getPgRef()).startsWith("FAKE-PG-");
        });
    }

    @DisplayName("confirm은 prepare 금액과 다른 금액이면 도메인 저장 전에 거부한다")
    @Test
    void confirm_amountTampered_rejectsBeforeFulfillment() {
        User user = userStorePort.save(new User("payment-tamper@example.com", "hashed", "회원", "01087654321"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        assertThatThrownBy(() -> confirmUseCase.confirm(
                new ConfirmCommand("payment-key-tampered", prepared.orderId(), prepared.amount() - 1, auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, e ->
                        assertSoftly(softly -> {
                            softly.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            softly.assertThat(e.getMessage()).contains("결제 금액");
                        }));

        assertSoftly(softly -> {
            softly.assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                    .hasValueSatisfying(attempt -> softly.assertThat(attempt.getStatus())
                            .isEqualTo(PaymentAttemptStatus.PENDING));
        });
    }
}
